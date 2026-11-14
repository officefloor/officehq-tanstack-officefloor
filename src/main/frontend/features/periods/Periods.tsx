import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { claimsKey, counts, listClaims } from '../claims/api';
import { listPeople, peopleKey } from '../people/api';
import { claimDue, listRates, ratesKey, sumDues } from '../rates/api';
import { addPeriod, closePeriod, listPeriods, periodsKey, type PeriodInput } from './api';

const EMPTY: PeriodInput = { name: '', from: '', to: '' };

/** The sheet's periods: a form that adds one, and each period by from-date with its due-total (the
 *  due of the claims that count whose date falls in it, n/a ones skipped; once closed, the one fixed
 *  when it was closed), whether it is closed, and a button that closes it. */
export function Periods({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const periods = useQuery({ queryKey: periodsKey(sheetId), queryFn: () => listPeriods(sheetId) });
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
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

  // The period whose last close was refused; cleared by the next close.
  const close = useMutation({
    mutationFn: (name: string) => closePeriod(sheetId, user, name),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) }),
  });

  const closeOne = (name: string) => {
    actOnce(() => {
      close.reset();
      return close.mutateAsync(name);
    });
  };

  const change = (patch: Partial<PeriodInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    save.reset();
  };

  const gradeOf = new Map((people.data ?? []).map((p) => [p.id, p.grade]));
  const counting = (claims.data ?? []).filter(counts);
  const dueTotal = (from: string, to: string) =>
    sumDues(counting.filter((c) => from <= c.date && c.date <= to)
      .map((c) => claimDue(c, rates.data, gradeOf.get(c.personId) ?? '')));

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
                <button type="button" data-testid="period-close" disabled={close.isPending}
                  onClick={() => closeOne(p.name)}>
                  Close
                </button>
                {close.isError && close.variables === p.name && (
                  <span data-testid="period-error">The period is already closed.</span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
