import { useState, type ChangeEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { SheetBody } from '../../slots/defs/sheetBody';
import { counts, fixedDue, useClaims } from '../claims/useClaims';
import { usePeople } from '../people/usePeople';
import { due, rateFor, sumDues, useRates } from '../rates/useRates';
import { periodOf, periodsKey, usePeriods, type Period } from './usePeriods';

/** The sheet's periods: a form that adds one (a name unique on the sheet, a from- and to-date,
 *  inclusive, overlapping no other period), and each period with its due-total: the figures of the
 *  claims that count whose date falls in it (a settled claim's frozen due, else the figure its last
 *  statement bound it to, else its live due), skipping those with none. */
function Periods({ sheetId }: { sheetId: string }) {
  const periods = usePeriods(sheetId);
  const claims = useClaims(sheetId);
  const people = usePeople(sheetId);
  const rates = useRates(sheetId);
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
    const grades = new Map((people.data ?? []).map((person) => [person.id, person.grade]));
    return sumDues(
      (claims.data ?? [])
        .filter((c) => counts(c) && periodOf([p], c.date) !== undefined)
        .map((c) => fixedDue(c) ?? due(c.amount, rateFor(rates.data, grades.get(c.personId), c.date))),
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
          </tr>
        </thead>
        <tbody>
          {(periods.data ?? []).map((p) => (
            <tr key={p.id} data-testid={`period-row-${p.name}`}>
              <td data-testid="period-name">{p.name}</td>
              <td data-testid="period-from">{p.from}</td>
              <td data-testid="period-to">{p.to}</td>
              <td data-testid="period-due-total">{dueTotal(p)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {periods.isError && <p data-testid="periods-error">{periods.error.message}</p>}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 6, Component: ({ sheetId }) => <Periods sheetId={sheetId} /> });
