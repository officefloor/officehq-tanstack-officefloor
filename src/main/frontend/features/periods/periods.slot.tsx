import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { usePeople } from '../people/people';
import { useClaims } from '../claims/claims';
import { useRates } from '../rates/rates';
import { useSettings } from '../roles/roles';
import { fiscalYearOf, periodClosePath, periodDue, periodLockPath, periodsGenerateFyPath, periodsGenerateQuartersPath, periodUnlockPath, periodReopenPath, periodSoftClosePath, periodsGeneratePath, periodsPath, periodsQueryKey, usePeriods, type Period } from './periods';

// The sheet's periods, each with its due-total — the dues (with any late charge) of the counted claims whose date falls in it (not a person's unbound OPEN ones dated within a hold of theirs),
// added up, skipping n/a; for a closed period, the one fixed when it was closed — whether it is closed, the
// buttons that close and reopen it, the form that adds one, and the form that generates one per calendar month
// (named yyyy-mm, leaving out a month whose name or days the sheet's periods already take), showing how many were made,
// a cap on each person's open claims in it (blank for none), and the entry that generates a fiscal year's twelve the same way, and the entry that generates a calendar year's four quarters (yyyy-Q1 to yyyy-Q4,
// leaving out one whose name or days the sheet's periods already take); each period shows the fiscal year its from-date falls in.
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
  const [cap, setCap] = useState('');
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: (body: { name: string; from: string; to: string; cap: string }) =>
      postJson<Period>(periodsPath(sheet ?? ''), body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: periodsQueryKey(sheet ?? '') });
      setName('');
      setFrom('');
      setTo('');
      setCap('');
    },
  });
  const submit = useSubmitOnce(add);
  const [genFrom, setGenFrom] = useState('');
  const [genTo, setGenTo] = useState('');
  const generate = useMutation({
    mutationFn: (body: { from: string; to: string }) =>
      postJson<{ generated: number }>(periodsGeneratePath(sheet ?? ''), body, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: periodsQueryKey(sheet ?? '') }),
  });
  const submitGenerate = useSubmitOnce(generate);
  const [fyYear, setFyYear] = useState('');
  const generateFy = useMutation({
    mutationFn: (body: { year: string }) =>
      postJson<{ generated: number }>(periodsGenerateFyPath(sheet ?? ''), body, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: periodsQueryKey(sheet ?? '') }),
  });
  const submitGenerateFy = useSubmitOnce(generateFy);
  const [qYear, setQYear] = useState('');
  const generateQuarters = useMutation({
    mutationFn: (body: { year: string }) =>
      postJson<{ generated: number }>(periodsGenerateQuartersPath(sheet ?? ''), body, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: periodsQueryKey(sheet ?? '') }),
  });
  const submitGenerateQuarters = useSubmitOnce(generateQuarters);
  // The generators share one count and one error: the one pressed last shows.
  const generated = generate.isSuccess ? generate.data.generated
    : generateFy.isSuccess ? generateFy.data.generated
    : generateQuarters.isSuccess ? generateQuarters.data.generated : null;
  const resetGenerators = () => { generate.reset(); generateFy.reset(); generateQuarters.reset(); };

  if (!user || !sheet) return null;
  const dueTotal = (period: Period) =>
    periodDue(period, people.data, claims.data ?? [], rates.data, settings.data, today);
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
            <th>Fiscal year</th>
            <th>Cap</th>
            <th>Locked</th>
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
              <td data-testid="period-closed">{p.closed ? 'yes' : p.softClosed ? 'soft' : 'no'}</td>
              <td data-testid="period-fy">{fiscalYearOf(p.from, settings.data)}</td>
              <td data-testid="period-cap">{p.cap ?? ''}</td>
              <td data-testid="period-locked">{p.locked ? 'yes' : 'no'}</td>
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
          submit({ name, from, to, cap });
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
        <input
          data-testid="period-form-cap"
          placeholder="Open claims cap per person (blank for none)"
          inputMode="numeric"
          value={cap}
          onChange={(e) => { setCap(e.target.value); add.reset(); }}
        />
        <button type="submit" data-testid="period-form-submit" disabled={add.isPending}>
          Add period
        </button>
        {add.isError && (
          <span data-testid="period-form-error">A period needs a name the sheet does not have yet, and calendar from and to dates, the to-date on or after the from-date, not overlapping another period, and a cap that is blank or a whole number of at least 1.</span>
        )}
      </form>
      <form
        data-testid="period-generate-form"
        onSubmit={(e) => {
          e.preventDefault();
          generateFy.reset();
          generateQuarters.reset();
          submitGenerate({ from: genFrom, to: genTo });
        }}
      >
        <input
          data-testid="period-generate-from"
          placeholder="From (YYYY-MM-DD)"
          value={genFrom}
          onChange={(e) => { setGenFrom(e.target.value); resetGenerators(); }}
        />
        <input
          data-testid="period-generate-to"
          placeholder="To (YYYY-MM-DD)"
          value={genTo}
          onChange={(e) => { setGenTo(e.target.value); resetGenerators(); }}
        />
        <button type="submit" data-testid="period-generate" disabled={generate.isPending}>
          Generate monthly periods
        </button>
        {generated !== null && (
          <>
            {' '}Made <span data-testid="period-generated">{generated}</span>
          </>
        )}
        {generate.isError && (
          <span data-testid="period-generate-error">Nothing was made: the from and to dates must be calendar dates, the to-date on or after the from-date.</span>
        )}
      </form>
      <form
        data-testid="period-generate-fy-form"
        onSubmit={(e) => {
          e.preventDefault();
          generate.reset();
          generateQuarters.reset();
          submitGenerateFy({ year: fyYear });
        }}
      >
        <input
          data-testid="period-generate-fy-year"
          placeholder="Fiscal year (yyyy)"
          inputMode="numeric"
          value={fyYear}
          onChange={(e) => { setFyYear(e.target.value); resetGenerators(); }}
        />
        <button type="submit" data-testid="period-generate-fy" disabled={generateFy.isPending}>
          Generate fiscal year
        </button>
        {generateFy.isError && (
          <span data-testid="period-generate-error">Nothing was made: the fiscal year must be a year (yyyy).</span>
        )}
      </form>
      <form
        data-testid="period-generate-quarters-form"
        onSubmit={(e) => {
          e.preventDefault();
          generate.reset();
          generateFy.reset();
          submitGenerateQuarters({ year: qYear });
        }}
      >
        <input
          data-testid="period-generate-quarters-year"
          placeholder="Year (yyyy)"
          inputMode="numeric"
          value={qYear}
          onChange={(e) => { setQYear(e.target.value); resetGenerators(); }}
        />
        <button type="submit" data-testid="period-generate-quarters" disabled={generateQuarters.isPending}>
          Generate quarters
        </button>
        {generateQuarters.isError && (
          <span data-testid="period-generate-error">Nothing was made: the year must be a year (yyyy).</span>
        )}
      </form>
    </section>
  );
}

// The buttons that close, soft-close and reopen a period. Closing one already closed, soft-closing one closed or
// soft-closed (or by a role not allowed to close), or reopening one not closed, or either by
// a role the permission matrix does not allow (by the standing rule, reopening by anyone but a steward), is refused by the server and shows the period's error, which goes as soon as the next
// action on the row is taken. Locking (one not locked) and unlocking (one locked) are a steward's only; a locked period
// cannot be reopened.
function PeriodActions({ sheet, period }: { sheet: string; period: Period }) {
  const [user] = useCurrentUser();
  const queryClient = useQueryClient();
  const onSuccess = () => queryClient.invalidateQueries({ queryKey: ['sheet', sheet] });
  const close = useMutation({
    mutationFn: () => putJson<Period>(periodClosePath(sheet, period.id), undefined, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: periodsQueryKey(sheet) }),
  });
  const softClose = useMutation({
    mutationFn: () => putJson<Period>(periodSoftClosePath(sheet, period.id), undefined, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: periodsQueryKey(sheet) }),
  });
  const reopen = useMutation({
    mutationFn: () => putJson<Period>(periodReopenPath(sheet, period.id), undefined, user),
    onSuccess,
  });
  const lock = useMutation({
    mutationFn: () => putJson<Period>(periodLockPath(sheet, period.id), undefined, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: periodsQueryKey(sheet) }),
  });
  const unlock = useMutation({
    mutationFn: () => putJson<Period>(periodUnlockPath(sheet, period.id), undefined, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: periodsQueryKey(sheet) }),
  });
  const submitClose = useSubmitOnce(close);
  const submitSoftClose = useSubmitOnce(softClose);
  const submitReopen = useSubmitOnce(reopen);
  const submitLock = useSubmitOnce(lock);
  const submitUnlock = useSubmitOnce(unlock);
  const pending = close.isPending || softClose.isPending || reopen.isPending || lock.isPending || unlock.isPending;
  const resetAll = () => { close.reset(); softClose.reset(); reopen.reset(); lock.reset(); unlock.reset(); };
  return (
    <>
      <button type="button" data-testid="period-close" disabled={pending}
        onClick={() => { resetAll(); submitClose(); }}>
        Close
      </button>
      <button type="button" data-testid="period-soft-close" disabled={pending}
        onClick={() => { resetAll(); submitSoftClose(); }}>
        Soft close
      </button>
      <button type="button" data-testid="period-reopen" disabled={pending}
        onClick={() => { resetAll(); submitReopen(); }}>
        Reopen
      </button>
      <button type="button" data-testid="period-lock" disabled={pending}
        onClick={() => { resetAll(); submitLock(); }}>
        Lock
      </button>
      <button type="button" data-testid="period-unlock" disabled={pending}
        onClick={() => { resetAll(); submitUnlock(); }}>
        Unlock
      </button>
      {close.isError && <span data-testid="period-error">This period cannot be closed: it is already closed, or your role is not allowed to close one.</span>}
      {softClose.isError && <span data-testid="period-error">This period cannot be soft-closed: it is already closed or soft-closed, or your role is not allowed to close one.</span>}
      {reopen.isError && <span data-testid="period-error">This period cannot be reopened: it is not closed or soft-closed, it is locked, or your role is not allowed to reopen one.</span>}
      {lock.isError && <span data-testid="period-error">This period cannot be locked: it is already locked, or you are not a steward.</span>}
      {unlock.isError && <span data-testid="period-error">This period cannot be unlocked: it is not locked, or you are not a steward.</span>}
    </>
  );
}

export const contribution = HomeMain.fill({ order: 6, Component: Periods });
