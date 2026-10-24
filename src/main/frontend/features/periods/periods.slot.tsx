import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { usePeople } from '../people/people';
import { counts, useClaims } from '../claims/claims';
import { figureOf, sumDue, useRates } from '../rates/rates';
import { periodOn, periodsPath, periodsQueryKey, usePeriods, type Period } from './periods';

// The sheet's periods, each with its due-total — the figures of the counted claims whose date falls in it,
// added up, skipping n/a — and the form that adds one.
function Periods() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const periods = usePeriods();
  const people = usePeople();
  const claims = useClaims();
  const rates = useRates();
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
  const dueTotal = (period: Period) =>
    sumDue((claims.data ?? [])
      .filter((c) => counts(c) && gradeOf.has(c.personId) && periodOn([period], c.date))
      .map((c) => figureOf(c, rates.data, gradeOf.get(c.personId) ?? '')));
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

export const contribution = HomeMain.fill({ order: 6, Component: Periods });
