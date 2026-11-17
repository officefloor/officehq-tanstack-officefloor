import { Fragment, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { SheetPage } from '../../slots/defs/sheetPage';
import { useClaims } from '../claims/claims';
import { usePeople, type Person } from '../people/people';
import { useRates } from '../rates/rates';
import { useFiscalStartMonth, useFloor, useLate } from '../settings/settings';
import { fiscalYearOf, periodDue, periodsKey, personPeriodCarried, personPeriodDue, usePeriods, type Period } from './periods';

type Values = { name: string; from: string; to: string; cap: string };
const EMPTY: Values = { name: '', from: '', to: '', cap: '' };

/** The sheet's periods: the form that adds one, and each period with its due-total — the figures of the claims
 *  that count, each with its late charge (a person's OPEN or PENDING claims dated within a hold of theirs, not bound by a statement, do not) whose due-by falls in it, added up (skipping n/a); a closed period
 *  shows the due-total fixed when it was closed, a soft-closed one that fixed when it was soft-closed. Each period
 *  shows the fiscal year its from-date falls in (see fiscalYearOf), and its cap on each person's OPEN or PENDING claims in it (blank: none). */
function Periods({ sheetId, user }: { sheetId: string; user: string }) {
  const periods = usePeriods(sheetId);
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const fiscalStart = useFiscalStartMonth(sheetId);
  const queryClient = useQueryClient();
  const [values, setValues] = useState<Values>(EMPTY);

  const submit = useSubmitOnce();
  const save = useMutation({
    mutationFn: (v: Values) =>
      postJson<Period>(`/api/sheets/${encodeURIComponent(sheetId)}/periods`, { ...v, by: user }),
    onSuccess: () => {
      setValues(EMPTY);
      void queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
    onSettled: submit.end,
  });
  const change = (v: Partial<Values>) => {
    setValues((was) => ({ ...was, ...v }));
    save.reset();
  };

  return (
    <section data-testid="periods">
      <h3>Periods</h3>
      <form
        data-testid="period-form"
        onSubmit={(e) => {
          e.preventDefault();
          if (!submit.begin()) return;
          save.reset();
          save.mutate(values);
        }}
      >
        <input data-testid="period-form-name" placeholder="Name" value={values.name} onChange={(e) => change({ name: e.target.value })} />{' '}
        <label>
          From <input type="date" data-testid="period-form-from" value={values.from} onChange={(e) => change({ from: e.target.value })} />
        </label>{' '}
        <label>
          To <input type="date" data-testid="period-form-to" value={values.to} onChange={(e) => change({ to: e.target.value })} />
        </label>{' '}
        <label>
          Cap <input data-testid="period-form-cap" inputMode="numeric" placeholder="none" value={values.cap} onChange={(e) => change({ cap: e.target.value })} />
        </label>{' '}
        <button type="submit" data-testid="period-form-submit" disabled={save.isPending}>
          Add period
        </button>
        {save.isError && (
          <span data-testid="period-form-error">
            A period needs a name not already on the sheet, and dates from on or before to that overlap no other period, and a cap (if any) that is a whole number above 0.
          </span>
        )}
      </form>
      <PeriodGenerate sheetId={sheetId} user={user} />
      <Rollover sheetId={sheetId} user={user} />
      {periods.isError && <p data-testid="periods-load-error">Could not load periods.</p>}
      <table data-testid="periods-table">
        <tbody>
          {(periods.data ?? []).map((p) => (
            <tr key={p.id} data-testid={`period-row-${p.name}`}>
              <td data-testid="period-name">{p.name}</td>
              <td data-testid="period-from">{p.from}</td>
              <td data-testid="period-to">{p.to}</td>
              <td data-testid="period-due-total">{periodDue(p, claims.data, people.data, rates.data, floor, late)}</td>
              <td data-testid="period-closed">{p.closed ? 'yes' : p.soft ? 'soft' : 'no'}</td>
              <td data-testid="period-fy">{fiscalYearOf(p.from, fiscalStart)}</td>
              <td data-testid="period-cap">{p.cap ?? ''}</td>
              <td data-testid="period-locked">{p.locked ? 'yes' : 'no'}</td>
              <td>
                <PeriodClose sheetId={sheetId} user={user} period={p} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

type Range = { from: string; to: string };

/** Generates one period per calendar month from the from-date's month to the to-date's month, each from the first to
 *  the last day of its month and named yyyy-mm; a month whose name is already a period, or whose days overlap one, is
 *  left out. Or generates a fiscal year's twelve monthly periods the same way: the year (yyyy) is the one its first
 *  day falls in, starting with the sheet's fiscal start month. Or generates a calendar year's four quarters (yyyy) as
 *  periods named yyyy-Q1 to yyyy-Q4, one left out the same way. Shows how many the last press made; the count is
 *  cleared while a new press of either runs. A to-date before the from-date, or a year that is not yyyy, is refused. */
function PeriodGenerate({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [range, setRange] = useState<Range>({ from: '', to: '' });
  const [year, setYear] = useState('');
  const [quartersYear, setQuartersYear] = useState('');
  const onSuccess = async () => {
    await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
  };
  const generate = useMutation({
    mutationFn: (r: Range) =>
      postJson<{ made: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/periods/generate`, { ...r, by: user }),
    onSuccess,
  });
  const generateFy = useMutation({
    mutationFn: (y: string) =>
      postJson<{ made: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/periods/generate-fy`, { year: y, by: user }),
    onSuccess,
  });
  const generateQuarters = useMutation({
    mutationFn: (y: string) =>
      postJson<{ made: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/periods/generate-quarters`, { year: y, by: user }),
    onSuccess,
  });
  const resetAll = () => {
    generate.reset();
    generateFy.reset();
    generateQuarters.reset();
  };
  const change = (r: Partial<Range>) => {
    setRange((was) => ({ ...was, ...r }));
    resetAll();
  };
  const pending = generate.isPending || generateFy.isPending || generateQuarters.isPending;
  const made = generate.isSuccess
    ? generate.data.made
    : generateFy.isSuccess
      ? generateFy.data.made
      : generateQuarters.isSuccess
        ? generateQuarters.data.made
        : undefined;
  return (
    <p>
      Generate monthly periods{' '}
      <label>
        From <input type="date" data-testid="period-generate-from" value={range.from} onChange={(e) => change({ from: e.target.value })} />
      </label>{' '}
      <label>
        To <input type="date" data-testid="period-generate-to" value={range.to} onChange={(e) => change({ to: e.target.value })} />
      </label>{' '}
      <button
        type="button"
        data-testid="period-generate"
        disabled={pending}
        onClick={() => {
          resetAll();
          generate.mutate(range);
        }}
      >
        Generate
      </button>{' '}
      or fiscal year{' '}
      <input
        data-testid="period-generate-fy-year"
        inputMode="numeric"
        placeholder="yyyy"
        value={year}
        onChange={(e) => {
          setYear(e.target.value);
          resetAll();
        }}
      />{' '}
      <button
        type="button"
        data-testid="period-generate-fy"
        disabled={pending}
        onClick={() => {
          resetAll();
          generateFy.mutate(year);
        }}
      >
        Generate fiscal year
      </button>{' '}
      or quarters of{' '}
      <input
        data-testid="period-generate-quarters-year"
        inputMode="numeric"
        placeholder="yyyy"
        value={quartersYear}
        onChange={(e) => {
          setQuartersYear(e.target.value);
          resetAll();
        }}
      />{' '}
      <button
        type="button"
        data-testid="period-generate-quarters"
        disabled={pending}
        onClick={() => {
          resetAll();
          generateQuarters.mutate(quartersYear);
        }}
      >
        Generate quarters
      </button>
      {made !== undefined && (
        <>
          {' '}
          Made <span data-testid="period-generated">{made}</span>
        </>
      )}
      {generate.isError && <span data-testid="period-generate-error"> The to-date may not be before the from-date.</span>}
      {generateFy.isError && <span data-testid="period-generate-error"> The fiscal year must be a year yyyy.</span>}
      {generateQuarters.isError && <span data-testid="period-generate-error"> The year must be a year yyyy.</span>}
    </p>
  );
}

/** Rolls a fiscal year (yyyy) over (a steward only): generates the next fiscal year's twelve periods as the fiscal year
 *  generator does, and adds each person's open figure at the end of the old year's last period to their carried figure
 *  for the new year's first period. Shows how many periods the last press made; the count is cleared while a new press
 *  runs. A year that is not yyyy, a year with no periods, or anyone but a steward is refused. */
function Rollover({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [year, setYear] = useState('');
  const rollover = useMutation({
    mutationFn: (y: string) =>
      postJson<{ made: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/periods/rollover`, { year: y, by: user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
  });
  return (
    <p>
      Roll year over{' '}
      <input
        data-testid="rollover-year"
        inputMode="numeric"
        placeholder="yyyy"
        value={year}
        onChange={(e) => {
          setYear(e.target.value);
          rollover.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="rollover"
        disabled={rollover.isPending}
        onClick={() => {
          rollover.reset();
          rollover.mutate(year);
        }}
      >
        Roll over
      </button>
      {rollover.isSuccess && (
        <>
          {' '}
          Made <span data-testid="rollover-made">{rollover.data.made}</span>
        </>
      )}
      {rollover.isError && (
        <span data-testid="rollover-error"> Only a steward can roll a year over, and only a year yyyy that has periods.</span>
      )}
    </p>
  );
}

/** One header cell per period of the sheet. */
function PeriodsHead({ sheetId }: { sheetId: string }) {
  const periods = usePeriods(sheetId);
  return (
    <>
      {(periods.data ?? []).map((p) => (
        <Fragment key={p.id}>
          <th>{p.name} due</th>
          <th>{p.name} carried</th>
        </Fragment>
      ))}
    </>
  );
}

/** A person's due-total and carried figure for each period of the sheet (see personPeriodDue, personPeriodCarried). */
function PersonPeriods({ sheetId, person }: { sheetId: string; person: Person }) {
  const periods = usePeriods(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  return (
    <>
      {(periods.data ?? []).map((p) => (
        <Fragment key={p.id}>
          <td data-testid={`person-period-${p.name}-due-total`}>
            {personPeriodDue(p, person, claims.data, rates.data, floor, late)}
          </td>
          <td data-testid={`person-period-${p.name}-carried`}>
            {personPeriodCarried(p, periods.data, person, claims.data, rates.data, floor, late)}
          </td>
        </Fragment>
      ))}
    </>
  );
}

export const contributions = [
  SheetPage.fill({ order: 7, Component: Periods }),
  PeopleHead.fill({ order: 22, Component: PeriodsHead }),
  PersonRow.fill({ order: 22, Component: PersonPeriods }),
];

/** Closes a period: its due-total is fixed as it is now. Soft-closes an open one: its due-total is fixed as it is now, but
 *  claims dated inside it can still change (both by the roles the close permission allows). Reopens a closed or soft-closed one (the roles the reopen permission allows; a steward while blank): its due-total is
 *  live again, unless it is locked. Locks or unlocks one (a steward only); a locked period cannot be reopened. A refused action shows the period's error; the next action on the period clears it. */
function PeriodClose({ sheetId, user, period }: { sheetId: string; user: string; period: Period }) {
  const queryClient = useQueryClient();
  const act = (action: 'close' | 'soft-close' | 'reopen' | 'lock' | 'unlock') =>
    postJson<Period>(`/api/sheets/${encodeURIComponent(sheetId)}/periods/${period.id}/${action}`, { by: user });
  const onSuccess = () => {
    void queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
  };
  const close = useMutation({ mutationFn: () => act('close'), onSuccess });
  const softClose = useMutation({ mutationFn: () => act('soft-close'), onSuccess });
  const reopen = useMutation({ mutationFn: () => act('reopen'), onSuccess });
  const lock = useMutation({ mutationFn: () => act('lock'), onSuccess });
  const unlock = useMutation({ mutationFn: () => act('unlock'), onSuccess });
  const pending = close.isPending || softClose.isPending || reopen.isPending || lock.isPending || unlock.isPending;
  const resetAll = () => {
    close.reset();
    softClose.reset();
    reopen.reset();
    lock.reset();
    unlock.reset();
  };
  return (
    <>
      <button
        type="button"
        data-testid="period-close"
        disabled={pending}
        onClick={() => {
          resetAll();
          close.mutate();
        }}
      >
        Close
      </button>{' '}
      <button
        type="button"
        data-testid="period-soft-close"
        disabled={pending}
        onClick={() => {
          resetAll();
          softClose.mutate();
        }}
      >
        Soft close
      </button>{' '}
      <button
        type="button"
        data-testid="period-reopen"
        disabled={pending}
        onClick={() => {
          resetAll();
          reopen.mutate();
        }}
      >
        Reopen
      </button>{' '}
      <button
        type="button"
        data-testid="period-lock"
        disabled={pending}
        onClick={() => {
          resetAll();
          lock.mutate();
        }}
      >
        Lock
      </button>{' '}
      <button
        type="button"
        data-testid="period-unlock"
        disabled={pending}
        onClick={() => {
          resetAll();
          unlock.mutate();
        }}
      >
        Unlock
      </button>
      {close.isError && <span data-testid="period-error"> This period is already closed, or your role may not close a period.</span>}
      {softClose.isError && <span data-testid="period-error"> Only an open period can be soft-closed, by a role the sheet lets close.</span>}
      {reopen.isError && <span data-testid="period-error"> Only a role the sheet lets reopen (a steward unless set) can reopen a period, and only a closed or soft-closed one that is not locked.</span>}
      {lock.isError && <span data-testid="period-error"> Only a steward can lock a period, and only one that is not locked.</span>}
      {unlock.isError && <span data-testid="period-error"> Only a steward can unlock a period, and only one that is locked.</span>}
    </>
  );
}
