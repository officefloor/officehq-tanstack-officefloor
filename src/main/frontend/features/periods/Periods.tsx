import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSubmitOnce } from '../../forms/useSubmitOnce';
import { claimsKey, listClaims } from '../claims/api';
import { listPeople, peopleKey } from '../people/api';
import { listRates, ratesKey } from '../rates/api';
import { fiscalYearOf, useFiscalStartMonth, useFloor, useLate } from '../settings/api';
import { useSheetToday } from '../sheet/api';
import { addPeriod, closePeriod, generateFiscalYear, generatePeriods, generateQuarters, listPeriods, lockPeriod, periodClaimsCount, periodDueTotal, periodsKey, reopenPeriod, softClosePeriod, type PeriodInput } from './api';
import { Fig } from '../session/Fig';
import { ImportOpenings } from './ImportOpenings';
import { Rollover } from './Rollover';

type PeriodAction = 'close' | 'soft-close' | 'reopen' | 'lock' | 'unlock';

const EMPTY: PeriodInput = { name: '', from: '', to: '', cap: '' };

/** The sheet's periods: a form that adds one, and each period by from-date with its due-total (the
 *  due of the claims that count whose date falls in it, n/a ones and OPEN unbound ones dated within their person's hold skipped; once closed, the one fixed
 *  when it was closed or soft-closed), the fiscal year its from-date falls in, its cap on each person's open claims (blank for none), whether it is closed (yes, soft or no), whether it is locked (yes or no), its claims count (VOID and DELETED ones left out), and buttons that close, soft-close and reopen it (each by the roles the permission matrix allows) and lock and unlock it (a steward only). */
export function Periods({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const periods = useQuery({ queryKey: periodsKey(sheetId), queryFn: () => listPeriods(sheetId) });
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const floor = useFloor(sheetId);
  const charges = useLate(sheetId, useSheetToday(sheetId, user));
  const fiscalStart = useFiscalStartMonth(sheetId);
  const [form, setForm] = useState<PeriodInput>(EMPTY);

  const save = useMutation({
    mutationFn: (input: PeriodInput) => addPeriod(sheetId, user, input),
    onSuccess: () => {
      setForm(EMPTY);
      return queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
  });

  // Generate monthly periods between two dates, a fiscal year's or a year's quarters; the one count shown
  // is cleared while it runs.
  const [range, setRange] = useState({ from: '', to: '' });
  const [fyYear, setFyYear] = useState('');
  const [quarterYear, setQuarterYear] = useState('');
  const generate = useMutation({
    mutationFn: (r: { from: string; to: string } | { year: string } | { quarters: string }) => 'quarters' in r
      ? generateQuarters(sheetId, user, r.quarters)
      : 'year' in r ? generateFiscalYear(sheetId, user, r.year) : generatePeriods(sheetId, user, r.from, r.to),
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
    mutationFn: ({ name, action }: { name: string; action: PeriodAction }) =>
      action === 'close' ? closePeriod(sheetId, user, name)
        : action === 'soft-close' ? softClosePeriod(sheetId, user, name)
          : action === 'lock' || action === 'unlock' ? lockPeriod(sheetId, user, name, action === 'lock')
            : reopenPeriod(sheetId, user, name),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) }),
  });

  const actOne = (name: string, action: PeriodAction) => {
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
        <label>
          Cap{' '}
          <input data-testid="period-form-cap" value={form.cap} inputMode="numeric"
            onChange={(e) => change({ cap: e.target.value })} />
        </label>{' '}
        <button type="submit" data-testid="period-form-submit" disabled={save.isPending}>
          Add period
        </button>
        {save.isError && (
          <span data-testid="period-form-error">A period needs a name not already in use, and from and to dates, from on or before to, not overlapping another period, and a cap (if any) that is a whole number above zero.</span>
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
          <span data-testid="period-generate-error">{generate.variables && 'quarters' in generate.variables
            ? 'The quarters\' year must be a year (yyyy).'
            : generate.variables && 'year' in generate.variables
              ? 'The fiscal year must be a year (yyyy).' : 'The to-date must be on or after the from-date.'}</span>
        )}
      </form>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          generateOnce(() => {
            generate.reset();
            return generate.mutateAsync({ year: fyYear });
          });
        }}
      >
        <label>
          Generate fiscal year FY{' '}
          <input data-testid="period-generate-fy-year" value={fyYear} placeholder="yyyy"
            onChange={(e) => { setFyYear(e.target.value); generate.reset(); }} />
        </label>{' '}
        <button type="submit" data-testid="period-generate-fy" disabled={generate.isPending}>
          Generate fiscal year
        </button>
      </form>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          generateOnce(() => {
            generate.reset();
            return generate.mutateAsync({ quarters: quarterYear });
          });
        }}
      >
        <label>
          Generate quarters of{' '}
          <input data-testid="period-generate-quarters-year" value={quarterYear} placeholder="yyyy"
            onChange={(e) => { setQuarterYear(e.target.value); generate.reset(); }} />
        </label>{' '}
        <button type="submit" data-testid="period-generate-quarters" disabled={generate.isPending}>
          Generate quarters
        </button>
      </form>
      <ImportOpenings sheetId={sheetId} user={user} />
      <Rollover sheetId={sheetId} user={user} />
      <table>
        <tbody>
          {(periods.data ?? []).map((p) => (
            <tr key={p.name} data-testid={`period-row-${p.name}`}>
              <td data-testid="period-name">{p.name}</td>
              <td data-testid="period-from">{p.from}</td>
              <td data-testid="period-to">{p.to}</td>
              <td>
                Due <span data-testid="period-due-total"><Fig>{periodDueTotal(p, claims.data ?? [], people.data, rates.data, floor, charges)}</Fig></span>
              </td>
              <td>
                <span data-testid="period-fy">{fiscalYearOf(p.from, fiscalStart)}</span>
              </td>
              <td>
                Claims <span data-testid="period-claims-count">{periodClaimsCount(p, claims.data ?? [])}</span>
              </td>
              <td>
                Cap <span data-testid="period-cap"><Fig>{p.cap ?? ''}</Fig></span>
              </td>
              <td>
                Closed <span data-testid="period-closed">{p.closed ? 'yes' : p.soft ? 'soft' : 'no'}</span>
              </td>
              <td>
                Locked <span data-testid="period-locked">{p.locked ? 'yes' : 'no'}</span>
              </td>
              <td>
                <button type="button" data-testid="period-close" disabled={act.isPending}
                  onClick={() => actOne(p.name, 'close')}>
                  Close
                </button>{' '}
                <button type="button" data-testid="period-soft-close" disabled={act.isPending}
                  onClick={() => actOne(p.name, 'soft-close')}>
                  Soft close
                </button>{' '}
                <button type="button" data-testid="period-reopen" disabled={act.isPending}
                  onClick={() => actOne(p.name, 'reopen')}>
                  Reopen
                </button>{' '}
                <button type="button" data-testid="period-lock" disabled={act.isPending}
                  onClick={() => actOne(p.name, 'lock')}>
                  Lock
                </button>{' '}
                <button type="button" data-testid="period-unlock" disabled={act.isPending}
                  onClick={() => actOne(p.name, 'unlock')}>
                  Unlock
                </button>
                {act.isError && act.variables?.name === p.name && (
                  <span data-testid="period-error">{act.variables.action === 'close'
                    ? 'Your role may not close a period, or it is already closed.'
                    : act.variables.action === 'soft-close'
                      ? 'Your role may not close a period, or it is already closed or soft-closed.'
                      : act.variables.action === 'lock'
                        ? 'Only a steward may lock a period, and it must not already be locked.'
                        : act.variables.action === 'unlock'
                          ? 'Only a steward may unlock a period, and it must be locked.'
                          : 'Your role may not reopen a period, or it is not closed or is locked.'}</span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
