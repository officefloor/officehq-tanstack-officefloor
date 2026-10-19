import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSingleSubmit } from '../../api/useSingleSubmit';
import { useCurrentUser } from '../session/SignIn';
import { claimDue, counts, useClaims } from '../claims/Claims';
import { usePeople } from '../people/People';
import { rateOn, total, useRates } from '../rates/Rates';
import { type Period, inPeriod, periodsKey, periodsPath, usePeriods } from './usePeriods';

// The sheet's periods, added here, each with its due-total: the figures of the claims that count
// whose date falls in it, each at its person's grade's rate in force on its date, skipping n/a.
export function Periods({ sheet }: { sheet: string }) {
  const periods = usePeriods(sheet);
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const gradeOf = new Map(people.map((p) => [p.id, p.grade]));
  const dueTotalOf = (period: Period) =>
    total(claims
      .filter((c) => counts(c) && inPeriod(period, c.date) && gradeOf.has(c.personId))
      .map((c) => claimDue(c, rateOn(rates, gradeOf.get(c.personId)!, c.date))));

  return (
    <section>
      <h2>Periods</h2>
      <PeriodForm sheet={sheet} />
      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>From</th>
            <th>To</th>
            <th>Due total</th>
          </tr>
        </thead>
        <tbody>
          {periods.map((p) => (
            <tr key={p.name} data-testid={`period-row-${p.name}`}>
              <td data-testid="period-name">{p.name}</td>
              <td data-testid="period-from">{p.from}</td>
              <td data-testid="period-to">{p.to}</td>
              <td data-testid="period-due-total">{dueTotalOf(p)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

function PeriodForm({ sheet }: { sheet: string }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [user] = useCurrentUser();
  const by = `?user=${encodeURIComponent(user ?? '')}`;
  const submitOnce = useSingleSubmit();
  const save = useMutation({
    mutationFn: (body: Period) => postJson<Period>(`${periodsPath(sheet)}${by}`, body),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheet) });
      setName('');
      setFrom('');
      setTo('');
    },
  });
  const changed = (set: (v: string) => void) => (v: string) => {
    save.reset();
    set(v);
  };

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        submitOnce(() => {
          save.reset();
          // A blank name or one in use, a missing date, a from after its to, or dates overlapping
          // another period is refused by the server (400).
          return save.mutateAsync({ name, from, to });
        });
      }}
    >
      <input data-testid="period-form-name" placeholder="Name" value={name}
        onChange={(e) => changed(setName)(e.target.value)} />{' '}
      <label>
        From{' '}
        <input type="date" data-testid="period-form-from" value={from}
          onChange={(e) => changed(setFrom)(e.target.value)} />
      </label>{' '}
      <label>
        To{' '}
        <input type="date" data-testid="period-form-to" value={to}
          onChange={(e) => changed(setTo)(e.target.value)} />
      </label>{' '}
      <button type="submit" data-testid="period-form-submit" disabled={save.isPending}>
        Add period
      </button>
      {save.isError && (
        <span role="alert" data-testid="period-form-error">
          {' '}
          {(save.error instanceof HttpError && save.error.status === 400 && save.error.reason)
            || 'The period could not be added.'}
        </span>
      )}
    </form>
  );
}
