import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { usePeople } from '../people/people';
import { twoDecimals, useRates } from '../rates/rates';
import { useFiscalStartMonth, useFloorCents } from '../settings/settings';
import { useClaims, useLate } from '../claims/claims';
import {
  closePeriodPath,
  lockPeriodPath,
  unlockPeriodPath,
  fiscalYearOf,
  generateFiscalYearPath,
  generatePeriodsPath,
  periodDueCents,
  shownDueTotal,
  reopenPeriodPath,
  softClosePeriodPath,
  periodsKey,
  periodsPath,
  usePeriods,
} from './periods';
import type { Generated, Period } from './periods';
import { Fig } from '../locale/Fig';

// One period with its due-total (fixed once closed or soft-closed) and closed flag (yes, soft or no), and the buttons
// that close, soft-close and reopen it. The server refuses closing a period already closed, soft-closing one already
// closed or soft-closed, and reopening one neither; and any of them by a role the permission matrix does not allow;
// the row shows that error until its next action. It also shows whether the period is locked, with the buttons that
// lock and unlock it; the server refuses either by anyone but a steward, locking one locked, unlocking one not, and
// reopening one locked.
// It also shows the fiscal year its from-date falls in.
function PeriodRow({ sheet, user, period, dueCents, fiscalStart }: { sheet: string; user: string; period: Period; dueCents: bigint; fiscalStart: number }) {
  const queryClient = useQueryClient();
  const close = useMutation({
    mutationFn: () => postJson<Period[]>(byUser(closePeriodPath(sheet, period.name), user), {}),
    onSuccess: (updated) => queryClient.setQueryData(periodsKey(sheet), updated),
  });
  const reopen = useMutation({
    mutationFn: () => postJson<Period[]>(byUser(reopenPeriodPath(sheet, period.name), user), {}),
    onSuccess: (updated) => queryClient.setQueryData(periodsKey(sheet), updated),
  });
  const softClose = useMutation({
    mutationFn: () => postJson<Period[]>(byUser(softClosePeriodPath(sheet, period.name), user), {}),
    onSuccess: (updated) => queryClient.setQueryData(periodsKey(sheet), updated),
  });
  const closeOnce = useSubmitOnce(close);
  const softCloseOnce = useSubmitOnce(softClose);
  const lock = useMutation({
    mutationFn: () => postJson<Period[]>(byUser(lockPeriodPath(sheet, period.name), user), {}),
    onSuccess: (updated) => queryClient.setQueryData(periodsKey(sheet), updated),
  });
  const unlock = useMutation({
    mutationFn: () => postJson<Period[]>(byUser(unlockPeriodPath(sheet, period.name), user), {}),
    onSuccess: (updated) => queryClient.setQueryData(periodsKey(sheet), updated),
  });
  const pending = close.isPending || softClose.isPending || reopen.isPending || lock.isPending || unlock.isPending;
  const reopenOnce = useSubmitOnce(reopen);
  const lockOnce = useSubmitOnce(lock);
  const unlockOnce = useSubmitOnce(unlock);
  // Clears every action's error but the one about to be taken.
  const resetAll = () => [close, softClose, reopen, lock, unlock].forEach((m) => m.reset());

  return (
    <tr data-testid={`period-row-${period.name}`}>
      <td data-testid="period-name">{period.name}</td>
      <td data-testid="period-from">{period.from}</td>
      <td data-testid="period-to">{period.to}</td>
      <td data-testid="period-due-total"><Fig>{shownDueTotal(period, dueCents)}</Fig></td>
      <td data-testid="period-closed">{period.closed ? 'yes' : period.soft ? 'soft' : 'no'}</td>
      <td data-testid="period-fy">{fiscalYearOf(period.from, fiscalStart)}</td>
      <td data-testid="period-cap"><Fig>{period.cap ?? ''}</Fig></td>
      <td data-testid="period-locked">{period.locked ? 'yes' : 'no'}</td>
      <td>
        <button
          type="button"
          data-testid="period-close"
          disabled={pending}
          onClick={() => {
            resetAll();
            closeOnce(undefined);
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
            softCloseOnce(undefined);
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
            reopenOnce(undefined);
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
            lockOnce(undefined);
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
            unlockOnce(undefined);
          }}
        >
          Unlock
        </button>
        {close.isError && (
          <span data-testid="period-error" role="alert">
            {' '}This period cannot be closed: it is already closed, or your role may not close it.
          </span>
        )}
        {softClose.isError && (
          <span data-testid="period-error" role="alert">
            {' '}This period cannot be soft-closed: it is already closed or soft-closed, or your role may not close it.
          </span>
        )}
        {reopen.isError && (
          <span data-testid="period-error" role="alert">
            {' '}This period cannot be reopened: it is not closed or soft-closed, it is locked, or your role may not reopen it.
          </span>
        )}
        {lock.isError && (
          <span data-testid="period-error" role="alert">
            {' '}This period cannot be locked: it is already locked, or only a steward may lock it.
          </span>
        )}
        {unlock.isError && (
          <span data-testid="period-error" role="alert">
            {' '}This period cannot be unlocked: it is not locked, or only a steward may unlock it.
          </span>
        )}
      </td>
    </tr>
  );
}

type Draft = { name: string; from: string; to: string; cap: string };
const EMPTY: Draft = { name: '', from: '', to: '', cap: '' };

type Range = { from: string; to: string };
const NO_RANGE: Range = { from: '', to: '' };

// Generates one period per calendar month from the from-date's month to the to-date's, named yyyy-mm; the server leaves
// out a month whose name is already a period or that overlaps one, and refuses a to-date before the from-date. The
// fiscal year entry generates that fiscal year's twelve months (from the sheet's fiscal start month in that year) the
// same way, and the server refuses a year that is not yyyy. Both share the one count of how many were made, and error.
type GenerateBody = { path: 'range'; range: Range } | { path: 'fy'; year: string };

function GeneratePeriods({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const [range, setRange] = useState<Range>(NO_RANGE);
  const [year, setYear] = useState('');
  const generate = useMutation({
    mutationFn: (body: GenerateBody) =>
      body.path === 'range'
        ? postJson<Generated>(byUser(generatePeriodsPath(sheet), user), body.range)
        : postJson<Generated>(byUser(generateFiscalYearPath(sheet), user), { year: body.year }),
    onSuccess: (result) => {
      queryClient.setQueryData(periodsKey(sheet), result.periods);
      setRange(NO_RANGE);
      setYear('');
    },
  });
  const send = useSubmitOnce(generate);

  const change = (next: Partial<Range>) => {
    setRange((r) => ({ ...r, ...next }));
    generate.reset();
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    send({ path: 'range', range });
  };

  const submitYear = (e: FormEvent) => {
    e.preventDefault();
    send({ path: 'fy', year });
  };

  return (
    <>
      <form onSubmit={submit}>
        <label>
          Generate monthly from{' '}
          <input
            data-testid="period-generate-from"
            placeholder="yyyy-mm-dd (blank: today)"
            value={range.from}
            onChange={(e) => change({ from: e.target.value })}
          />
        </label>{' '}
        <label>
          to{' '}
          <input
            data-testid="period-generate-to"
            placeholder="yyyy-mm-dd"
            value={range.to}
            onChange={(e) => change({ to: e.target.value })}
          />
        </label>{' '}
        <button type="submit" data-testid="period-generate" disabled={generate.isPending}>
          Generate periods
        </button>
        {generate.isSuccess && (
          <>
            {' '}Made <strong data-testid="period-generated">{generate.data.made}</strong>
          </>
        )}
        {generate.isError && (
          <span data-testid="period-generate-error" role="alert">
            {' '}Nothing was made: generating needs valid from and to dates, the to on or after the from, or a fiscal year (yyyy).
          </span>
        )}
      </form>
      <form onSubmit={submitYear}>
        <label>
          Generate fiscal year{' '}
          <input
            data-testid="period-generate-fy-year"
            placeholder="yyyy"
            value={year}
            onChange={(e) => {
              setYear(e.target.value);
              generate.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="period-generate-fy" disabled={generate.isPending}>
          Generate fiscal year
        </button>
      </form>
    </>
  );
}

// The sheet's periods, each with its due-total: the due now of the claims that counted at the end of its last day (see countedOn) whose date
// falls in it (a claim with no rate adds nothing), fixed when the period is closed. The form adds one; the server refuses a name already on
// the sheet, a bad date, a to-date before the from-date, dates that overlap another period, or a cap (open claims per
// person, blank for none) that is not a whole number of at least 1.
function Periods({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const { data: periods = [] } = usePeriods(sheet);
  const { data: all = [] } = useClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const { data: rates = [] } = useRates(sheet);
  const floor = useFloorCents(sheet);
  const late = useLate(sheet);
  const fiscalStart = useFiscalStartMonth(sheet);
  const [draft, setDraft] = useState<Draft>(EMPTY);

  const save = useMutation({
    mutationFn: (body: Draft) => postJson<Period[]>(byUser(periodsPath(sheet), user), body),
    onSuccess: (updated) => {
      queryClient.setQueryData(periodsKey(sheet), updated);
      setDraft(EMPTY);
    },
  });
  const send = useSubmitOnce(save);

  const change = (next: Partial<Draft>) => {
    setDraft((d) => ({ ...d, ...next }));
    save.reset();
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    send(draft);
  };

  const totals = periodDueCents(periods, all, people, rates, floor, late);

  return (
    <section>
      <h2>Periods</h2>
      <form onSubmit={submit}>
        <label>
          Name{' '}
          <input data-testid="period-form-name" value={draft.name} onChange={(e) => change({ name: e.target.value })} />
        </label>{' '}
        <label>
          From{' '}
          <input
            data-testid="period-form-from"
            placeholder="yyyy-mm-dd (blank: today)"
            value={draft.from}
            onChange={(e) => change({ from: e.target.value })}
          />
        </label>{' '}
        <label>
          To{' '}
          <input
            data-testid="period-form-to"
            placeholder="yyyy-mm-dd"
            value={draft.to}
            onChange={(e) => change({ to: e.target.value })}
          />
        </label>{' '}
        <label>
          Cap{' '}
          <input
            data-testid="period-form-cap"
            placeholder="open claims per person (blank: none)"
            value={draft.cap}
            onChange={(e) => change({ cap: e.target.value })}
          />
        </label>{' '}
        <button type="submit" data-testid="period-form-submit" disabled={save.isPending}>
          Add period
        </button>
        {save.isError && (
          <span data-testid="period-form-error" role="alert">
            {' '}A period needs a name not already on the sheet and valid from and to dates, the to on or after the from, that overlap no other period, and a cap that is blank or a whole number of at least 1.
          </span>
        )}
      </form>
      <GeneratePeriods sheet={sheet} user={user} />
      <table>
        <thead>
          <tr>
            <th>Period</th>
            <th>From</th>
            <th>To</th>
            <th>Due total</th>
            <th>Closed</th>
            <th>Fiscal year</th>
            <th>Cap</th>
            <th>Locked</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {periods.map((p) => (
            <PeriodRow key={p.name} sheet={sheet} user={user} period={p} dueCents={totals.get(p.name) ?? 0n} fiscalStart={fiscalStart} />
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 25, Component: Periods });
