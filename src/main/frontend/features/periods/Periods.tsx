import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { claimsKey, counts, listClaims, liveClaims } from '../claims/api';
import { gradeOn, listPeople, peopleKey } from '../people/api';
import { claimDue, listRates, ratesKey, sumDues } from '../rates/api';
import { useFloor, useLate } from '../settings/api';
import { useSheetToday } from '../sheet/api';
import { addPeriod, closePeriod, listPeriods, periodsKey, reopenPeriod, type PeriodInput } from './api';

const EMPTY: PeriodInput = { name: '', from: '', to: '' };

/** The sheet's periods: a form that adds one, and each period by from-date with its due-total (the
 *  due of the claims that count whose date falls in it, n/a ones and OPEN ones of people on hold skipped; once closed, the one fixed
 *  when it was closed), whether it is closed, and buttons that close it and (a steward only) reopen it. */
export function Periods({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const periods = useQuery({ queryKey: periodsKey(sheetId), queryFn: () => listPeriods(sheetId) });
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const floor = useFloor(sheetId);
  const charges = useLate(sheetId, useSheetToday(sheetId, user));
  const [form, setForm] = useState<PeriodInput>(EMPTY);

  const save = useMutation({
    mutationFn: (input: PeriodInput) => addPeriod(sheetId, user, input),
    onSuccess: () => {
      setForm(EMPTY);
      return queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
  });

  const submitOnce = useSubmitOnce();
  const actOnce = useSubmitOnce();

  // The period whose last close or reopen was refused; cleared by the next action.
  const act = useMutation({
    mutationFn: ({ name, action }: { name: string; action: 'close' | 'reopen' }) =>
      action === 'close' ? closePeriod(sheetId, user, name) : reopenPeriod(sheetId, user, name),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) }),
  });

  const actOne = (name: string, action: 'close' | 'reopen') => {
    actOnce(() => {
      act.reset();
      return act.mutateAsync({ name, action });
    });
  };

  const change = (patch: Partial<PeriodInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    save.reset();
  };

  const personOf = new Map((people.data ?? []).map((p) => [p.id, p]));
  const counting = liveClaims(claims.data ?? [], people.data).filter(counts);
  const dueTotal = (from: string, to: string) =>
    sumDues(counting.filter((c) => from <= c.date && c.date <= to)
      .map((c) => claimDue(c, rates.data, (d) => { const p = personOf.get(c.personId); return p ? gradeOn(p, d) : ''; }, floor, charges)));

  return (
    <section>
      <h3>Periods</h3>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            save.reset();
            return save.mutateAsync(form);
          });
        }}
      >
        <label>
          Name{' '}
          <input data-testid="period-form-name" value={form.name} onChange={(e) => change({ name: e.target.value })} />
        </label>{' '}
        <label>
          From{' '}
          <input type="date" data-testid="period-form-from" value={form.from}
            onChange={(e) => change({ from: e.target.value })} />
        </label>{' '}
        <label>
          To{' '}
          <input type="date" data-testid="period-form-to" value={form.to}
            onChange={(e) => change({ to: e.target.value })} />
        </label>{' '}
        <button type="submit" data-testid="period-form-submit" disabled={save.isPending}>
          Add period
        </button>
        {save.isError && (
          <span data-testid="period-form-error">A period needs a name not already in use, and from and to dates, from on or before to, not overlapping another period.</span>
        )}
      </form>
      <table>
        <tbody>
          {(periods.data ?? []).map((p) => (
            <tr key={p.name} data-testid={`period-row-${p.name}`}>
              <td data-testid="period-name">{p.name}</td>
              <td data-testid="period-from">{p.from}</td>
              <td data-testid="period-to">{p.to}</td>
              <td>
                Due <span data-testid="period-due-total">{p.closed && p.closedTotal !== null
                  ? p.closedTotal : dueTotal(p.from, p.to)}</span>
              </td>
              <td>
                Closed <span data-testid="period-closed">{p.closed ? 'yes' : 'no'}</span>
              </td>
              <td>
                <button type="button" data-testid="period-close" disabled={act.isPending}
                  onClick={() => actOne(p.name, 'close')}>
                  Close
                </button>{' '}
                <button type="button" data-testid="period-reopen" disabled={act.isPending}
                  onClick={() => actOne(p.name, 'reopen')}>
                  Reopen
                </button>
                {act.isError && act.variables?.name === p.name && (
                  <span data-testid="period-error">{act.variables.action === 'close'
                    ? 'The period is already closed.'
                    : 'Only a steward can reopen a period, and only a closed one.'}</span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
