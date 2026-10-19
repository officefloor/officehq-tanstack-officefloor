import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSingleSubmit } from '../../api/useSingleSubmit';
import { useCurrentUser } from '../session/SignIn';
import { claimDue, counts, useClaims, useLate } from '../claims/Claims';
import { gradeOn, usePeople } from '../people/People';
import { rateOn, total, useRates } from '../rates/Rates';
import { useFloor } from '../settings/Settings';
import { type Period, inPeriod, periodsKey, periodsPath, usePeriods } from './usePeriods';

// The sheet's periods, added here, each with its due-total: the figures of the claims that count
// (not a held person's OPEN claims)
// whose date falls in it, each at the rate in force on its date of its person's grade in force on that date, skipping n/a.
// A closed period shows the due-total it was closed at instead, whatever rates do later, until a steward reopens it.
export function Periods({ sheet }: { sheet: string }) {
  const periods = usePeriods(sheet);
  const dueTotalOf = usePeriodDueTotal(sheet);

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
            <th>Closed</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {periods.map((p) => (
            <PeriodRow key={p.name} sheet={sheet} period={p}
              dueTotal={dueTotalOf(p)} />
          ))}
        </tbody>
      </table>
    </section>
  );
}

/** A period's due-total as its row shows it: the one it was closed at while it is closed, else
 *  worked out from the claims that count. */
export function usePeriodDueTotal(sheet: string): (period: Period) => string {
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const floor = useFloor(sheet);
  const late = useLate(sheet);
  const personOf = new Map(people.map((p) => [p.id, p]));
  const held = new Set(people.filter((p) => p.held).map((p) => p.id));
  return (period: Period) =>
    period.closed && period.closedTotal != null ? period.closedTotal : total(claims
      .filter((c) => counts(c, held.has(c.personId)) && inPeriod(period, c.date) && personOf.has(c.personId))
      .map((c) => claimDue(c, rateOn(rates, gradeOn(personOf.get(c.personId)!, c.date), c.date), floor, late)));
}

/** One period: its dates, due-total and whether it is closed, with its close and reopen buttons. A refused close or reopen
 *  shows the row's error, which the row's next action clears. */
function PeriodRow({ sheet, period: p, dueTotal }: { sheet: string; period: Period; dueTotal: string }) {
  const queryClient = useQueryClient();
  const [user] = useCurrentUser();
  const by = `?user=${encodeURIComponent(user ?? '')}`;
  const close = useMutation({
    mutationFn: () => postJson<Period>(`${periodsPath(sheet)}/${encodeURIComponent(p.name)}/close${by}`, {}),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: periodsKey(sheet) }),
  });
  // Only a steward may reopen a closed period (the server refuses anyone else, and a period not closed).
  const reopen = useMutation({
    mutationFn: () => postJson<Period>(`${periodsPath(sheet)}/${encodeURIComponent(p.name)}/reopen${by}`, {}),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: periodsKey(sheet) }),
  });
  const failed = close.isError ? close.error : reopen.isError ? reopen.error : null;

  return (
    <tr data-testid={`period-row-${p.name}`}>
      <td data-testid="period-name">{p.name}</td>
      <td data-testid="period-from">{p.from}</td>
      <td data-testid="period-to">{p.to}</td>
      <td data-testid="period-due-total">{dueTotal}</td>
      <td data-testid="period-closed">{p.closed ? 'yes' : 'no'}</td>
      <td>
        <button type="button" data-testid="period-close" disabled={close.isPending}
          onClick={() => { close.reset(); reopen.reset(); close.mutate(); }}>
          Close
        </button>{' '}
        <button type="button" data-testid="period-reopen" disabled={reopen.isPending}
          onClick={() => { close.reset(); reopen.reset(); reopen.mutate(); }}>
          Reopen
        </button>
        {failed && (
          <span role="alert" data-testid="period-error">
            {' '}
            {(failed instanceof HttpError && failed.reason)
              || (close.isError ? 'The period could not be closed.' : 'The period could not be reopened.')}
          </span>
        )}
      </td>
    </tr>
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
