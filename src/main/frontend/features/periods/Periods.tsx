import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { claimsKey, listClaims } from '../claims/api';
import { listPeople, peopleKey } from '../people/api';
import { listRates, ratesKey } from '../rates/api';
import { useFloor, useLate } from '../settings/api';
import { useSheetToday } from '../sheet/api';
import { addPeriod, closePeriod, generatePeriods, listPeriods, periodDueTotal, periodsKey, reopenPeriod, type PeriodInput } from './api';

const EMPTY: PeriodInput = { name: '', from: '', to: '' };

/** The sheet's periods: a form that adds one, and each period by from-date with its due-total (the
 *  due of the claims that count whose date falls in it, n/a ones and OPEN unbound ones dated within their person's hold skipped; once closed, the one fixed
 *  when it was closed), whether it is closed, and buttons that close it and reopen it (each by the roles the permission matrix allows). */
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

  // Generate monthly periods between two dates; the count shown is cleared while it runs.
  const [range, setRange] = useState({ from: '', to: '' });
  const generate = useMutation({
    mutationFn: (r: { from: string; to: string }) => generatePeriods(sheetId, user, r.from, r.to),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) }),
  });
  const generateOnce = useSubmitOnce();
  const changeRange = (patch: Partial<{ from: string; to: string }>) => {
    setRange((r) => ({ ...r, ...patch }));
    generate.reset();
  };

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
      <form
        onSubmit={(e) => {
          e.preventDefault();
          generateOnce(() => {
            generate.reset();
            return generate.mutateAsync(range);
          });
        }}
      >
        <label>
          Generate monthly from{' '}
          <input type="date" data-testid="period-generate-from" value={range.from}
            onChange={(e) => changeRange({ from: e.target.value })} />
        </label>{' '}
        <label>
          to{' '}
          <input type="date" data-testid="period-generate-to" value={range.to}
            onChange={(e) => changeRange({ to: e.target.value })} />
        </label>{' '}
        <button type="submit" data-testid="period-generate" disabled={generate.isPending}>
          Generate periods
        </button>{' '}
        {generate.isSuccess && <>Made <span data-testid="period-generated">{generate.data.made}</span></>}
        {generate.isError && (
          <span data-testid="period-generate-error">The to-date must be on or after the from-date.</span>
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
                Due <span data-testid="period-due-total">{periodDueTotal(p, claims.data ?? [], people.data, rates.data, floor, charges)}</span>
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
                    ? 'Your role may not close a period, or it is already closed.'
                    : 'Your role may not reopen a period, or it is not closed.'}</span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
