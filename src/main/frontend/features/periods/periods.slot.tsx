import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { usePeople } from '../people/people';
import { countsLive, heldIds, useClaims } from '../claims/claims';
import { chargedOf, sumDue, useRates } from '../rates/rates';
import { useSettings } from '../roles/roles';
import { periodClosePath, periodOn, periodsPath, periodsQueryKey, usePeriods, type Period } from './periods';

// The sheet's periods, each with its due-total — the dues (with any late charge) of the counted claims whose date falls in it (not a held person's OPEN ones),
// added up, skipping n/a; for a closed period, the one fixed when it was closed — whether it is closed, the
// button that closes it, and the form that adds one.
function Periods() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const periods = usePeriods();
  const people = usePeople();
  const claims = useClaims();
  const rates = useRates();
  const settings = useSettings();
  const today = useSheetToday();
  const [name, setName] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: (body: { name: string; from: string; to: string }) =>
      postJson<Period>(periodsPath(sheet ?? ''), body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: periodsQueryKey(sheet ?? '') });
      setName('');
      setFrom('');
      setTo('');
    },
  });
  const submit = useSubmitOnce(add);

  if (!user || !sheet) return null;
  const gradeOf = new Map((people.data ?? []).map((p) => [p.id, p.grade]));
  const held = heldIds(people.data);
  const dueTotal = (period: Period) =>
    period.closedTotal ?? sumDue((claims.data ?? [])
      .filter((c) => countsLive(c, held) && gradeOf.has(c.personId) && periodOn([period], c.date))
      .map((c) => chargedOf(c, rates.data, gradeOf.get(c.personId) ?? '', settings.data, today)));
  return (
    <section data-testid="periods">
      <h2>Periods</h2>
      <table>
        <thead>
          <tr>
            <th>Period</th>
            <th>From</th>
            <th>To</th>
            <th>Due</th>
            <th>Closed</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {(periods.data ?? []).map((p) => (
            <tr key={p.id} data-testid={`period-row-${p.name}`}>
              <td data-testid="period-name">{p.name}</td>
              <td data-testid="period-from">{p.from}</td>
              <td data-testid="period-to">{p.to}</td>
              <td data-testid="period-due-total">{dueTotal(p)}</td>
              <td data-testid="period-closed">{p.closed ? 'yes' : 'no'}</td>
              <td>
                <PeriodActions key={`${sheet}:${p.id}`} sheet={sheet} period={p} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <form
        data-testid="period-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit({ name, from, to });
        }}
      >
        <input
          data-testid="period-form-name"
          placeholder="Name"
          value={name}
          onChange={(e) => { setName(e.target.value); add.reset(); }}
        />
        <input
          data-testid="period-form-from"
          placeholder="From (YYYY-MM-DD)"
          value={from}
          onChange={(e) => { setFrom(e.target.value); add.reset(); }}
        />
        <input
          data-testid="period-form-to"
          placeholder="To (YYYY-MM-DD)"
          value={to}
          onChange={(e) => { setTo(e.target.value); add.reset(); }}
        />
        <button type="submit" data-testid="period-form-submit" disabled={add.isPending}>
          Add period
        </button>
        {add.isError && (
          <span data-testid="period-form-error">A period needs a name the sheet does not have yet, and calendar from and to dates, the to-date on or after the from-date, not overlapping another period.</span>
        )}
      </form>
    </section>
  );
}

// The button that closes a period. Closing one already closed is refused by the server and shows the period's
// error, which goes as soon as the next action on the row is taken.
function PeriodActions({ sheet, period }: { sheet: string; period: Period }) {
  const [user] = useCurrentUser();
  const queryClient = useQueryClient();
  const close = useMutation({
    mutationFn: () => putJson<Period>(periodClosePath(sheet, period.id), undefined, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: periodsQueryKey(sheet) }),
  });
  const submitClose = useSubmitOnce(close);
  return (
    <>
      <button type="button" data-testid="period-close" disabled={close.isPending} onClick={() => submitClose()}>
        Close
      </button>
      {close.isError && <span data-testid="period-error">This period cannot be closed: it is already closed.</span>}
    </>
  );
}

export const contribution = HomeMain.fill({ order: 6, Component: Periods });
