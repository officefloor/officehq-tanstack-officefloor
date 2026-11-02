import { useState, type ChangeEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { SheetBody } from '../../slots/defs/sheetBody';
import { counts, fixedDue, useClaims } from '../claims/useClaims';
import { gradeOn, heldIds, usePeople } from '../people/usePeople';
import { due, rateFor, sumDues, useRates } from '../rates/useRates';
import { dueNow } from '../overdue/useOverdue';
import { useSettings } from '../settings/useSettings';
import { useSheetToday } from '../today/useSheetToday';
import { periodOf, periodsKey, usePeriods, type Period } from './usePeriods';

/** The sheet's periods: a form that adds one (a name unique on the sheet, a from- and to-date,
 *  inclusive, overlapping no other period), and each period with its due-total: the figures of the
 *  claims that count whose date falls in it (a settled claim's frozen due, else the figure its last
 *  statement bound it to, else its live due, with its late charge while overdue), skipping those with none — fixed once it is closed —
 *  and whether it is closed, with a button that closes it. */
function Periods({ sheetId }: { sheetId: string }) {
  const periods = usePeriods(sheetId);
  const claims = useClaims(sheetId);
  const people = usePeople(sheetId);
  const rates = useRates(sheetId);
  const settings = useSettings(sheetId).data;
  const floor = settings?.floor;
  const today = useSheetToday(sheetId).data?.today;
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');

  const submitOnce = useSingleFlight();
  const save = useMutation({
    mutationFn: (f: { name: string; from: string; to: string }) =>
      postJson<Period>(`/api/sheets/${encodeURIComponent(sheetId)}/periods`, f),
    onSuccess: async () => {
      setName('');
      setFrom('');
      setTo('');
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
  });

  const field = (set: (v: string) => void) => (e: ChangeEvent<HTMLInputElement>) => {
    set(e.target.value);
    save.reset();
  };

  const dueTotal = (p: Period) => {
    if (p.closed && p.closedTotal !== null) return p.closedTotal;
    const byId = new Map((people.data ?? []).map((person) => [person.id, person]));
    const held = heldIds(people.data);
    return sumDues(
      (claims.data ?? [])
        .filter((c) => counts(c, held) && periodOf([p], c.date) !== undefined)
        .map((c) => dueNow(c, fixedDue(c) ?? due(c.amount, rateFor(rates.data, gradeOn(byId.get(c.personId), c.date), c.date), floor), settings, today)),
    );
  };

  return (
    <section data-testid="periods">
      <form
        data-testid="period-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            save.reset();
            return save.mutateAsync({ name, from, to });
          });
        }}
      >
        <label>
          Period <input data-testid="period-form-name" value={name} onChange={field(setName)} />
        </label>{' '}
        <label>
          From{' '}
          <input type="text" placeholder="yyyy-mm-dd" data-testid="period-form-from" value={from} onChange={field(setFrom)} />
        </label>{' '}
        <label>
          To <input type="text" placeholder="yyyy-mm-dd" data-testid="period-form-to" value={to} onChange={field(setTo)} />
        </label>{' '}
        <button type="submit" data-testid="period-form-submit" disabled={save.isPending}>
          Add period
        </button>
        {save.isError && <p data-testid="period-form-error">{save.error.message}</p>}
      </form>
      <table data-testid="periods-list">
        <thead>
          <tr>
            <th>Period</th>
            <th>From</th>
            <th>To</th>
            <th>Due</th>
            <th>Closed</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {(periods.data ?? []).map((p) => (
            <PeriodRow key={p.id} sheetId={sheetId} period={p} dueTotal={dueTotal(p)} />
          ))}
        </tbody>
      </table>
      {periods.isError && <p data-testid="periods-error">{periods.error.message}</p>}
    </section>
  );
}

/** One period's row; closing it fixes its due-total. A refused close shows its error, cleared when
 *  the next action on the row is taken. */
function PeriodRow({ sheetId, period: p, dueTotal }: { sheetId: string; period: Period; dueTotal: string }) {
  const queryClient = useQueryClient();
  const closeOnce = useSingleFlight();
  const close = useMutation({
    mutationFn: () =>
      postJson<Period>(
        `/api/sheets/${encodeURIComponent(sheetId)}/periods/${encodeURIComponent(p.name)}/close`,
        {},
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
  });

  return (
    <tr data-testid={`period-row-${p.name}`}>
      <td data-testid="period-name">{p.name}</td>
      <td data-testid="period-from">{p.from}</td>
      <td data-testid="period-to">{p.to}</td>
      <td data-testid="period-due-total">{dueTotal}</td>
      <td data-testid="period-closed">{p.closed ? 'yes' : 'no'}</td>
      <td>
        <button
          type="button"
          data-testid="period-close"
          disabled={close.isPending}
          onClick={() =>
            closeOnce(() => {
              close.reset();
              return close.mutateAsync();
            })
          }
        >
          Close
        </button>
        {close.isError && <span data-testid="period-error">{close.error.message}</span>}
      </td>
    </tr>
  );
}

export const contribution = SheetBody.fill({ order: 6, Component: ({ sheetId }) => <Periods sheetId={sheetId} /> });
