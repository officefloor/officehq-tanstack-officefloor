import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { useClaims } from '../claims/useClaims';
import { usePeople } from '../people/usePeople';
import { useRates } from '../rates/useRates';
import { DEFAULT_FISCAL_START_MONTH, useFloor, useLate, useSettings } from '../settings/useSettings';
import { poolsKey } from '../pools/usePools';
import { useSheetToday } from '../sheet/useSheet';
import { CAP, DATE, fiscalYear, periodDue, periodsKey, usePeriods, YEAR, type Period } from './usePeriods';

/**
 * The sheet's periods, each with its due-total — the figures, as shown now, of everyone's claims
 * dated in it that were counted (OPEN, PENDING or SETTLED) at the end of its last day by their status
 * log, not deferred ones, nor ones open then and dated within their person's hold unless stated, n/a
 * ones skipped — and one form to add a period: a name unique on
 * the sheet and a from and to date, inclusive, overlapping no other period. A period can be closed:
 * its due-total is then the one it had when it was closed, whatever rates do later. A steward can
 * reopen a closed period, making its due-total live again. A period can instead be soft-closed: its
 * figures are fixed as by a close, but claims dated in it can still be changed; it can then be closed
 * (fixing them afresh) or reopened. A steward can lock a period, so it cannot be reopened, and unlock it. Each period shows the fiscal year its from-date falls in; a fiscal
 * year's twelve monthly periods can be generated as the monthly generator makes them, and a calendar
 * year's four quarters (yyyy-Q1 to yyyy-Q4) likewise.
 */
function PeriodsPanel({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const { data: periods = [] } = usePeriods(sheet);
  const { data: all = [] } = useClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const { data: rates = [] } = useRates(sheet);
  const floor = useFloor(sheet);
  const today = useSheetToday(sheet);
  const late = useLate(sheet);
  const fiscalStart = useSettings(sheet).data?.fiscalStartMonth ?? DEFAULT_FISCAL_START_MONTH;
  const [name, setName] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [cap, setCap] = useState('');
  const [invalid, setInvalid] = useState<string | null>(null);
  const [genFrom, setGenFrom] = useState('');
  const [genTo, setGenTo] = useState('');
  const [genInvalid, setGenInvalid] = useState<string | null>(null);
  const [fyYear, setFyYear] = useState('');
  const [qYear, setQYear] = useState('');
  // The period whose last close, reopen, lock or unlock was refused: its row shows the error.
  const [refused, setRefused] = useState<string | null>(null);

  const submitOnce = useSubmitOnce();
  const save = useMutation({
    mutationFn: (p: { name: string; from: string; to: string; cap: string }) =>
      postJson<Period>(`/api/sheets/${encodeURIComponent(sheet)}/periods`, { ...p, user }),
    onSuccess: async () => {
      setName('');
      setFrom('');
      setTo('');
      setCap('');
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheet) });
    },
  });

  // One period per calendar month from the from-date's month to the to-date's, named yyyy-mm; a
  // month already named or overlapping a period is left out. Answers how many were made.
  // A fiscal year generate makes the same periods for the fiscal year's twelve months; either shows
  // its count in the one place. A quarters generate makes a calendar year's four quarters, named
  // yyyy-Q1 to yyyy-Q4, the same way, and shows its count there too.
  const generate = useMutation({
    mutationFn: ({ kind, ...g }: { kind: 'periods' | 'fiscal-year' | 'quarters'; from?: string; to?: string; year?: string }) =>
      postJson<{ generated: number }>(`/api/sheets/${encodeURIComponent(sheet)}/generate-${kind}`, { ...g, user }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: periodsKey(sheet) }),
  });

  const clearGenerate = () => {
    generate.reset();
    setGenInvalid(null);
  };

  const close = useMutation({
    mutationFn: (name: string) =>
      postJson<Period>(`/api/sheets/${encodeURIComponent(sheet)}/periods/${encodeURIComponent(name)}/close`, { user }),
    onMutate: () => setRefused(null),
    onError: (_e, name) => setRefused(name),
    // A close fixes the pool members' cuts for the period; a reopen makes them live again.
    onSettled: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: periodsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: poolsKey(sheet) }),
      ]),
  });

  const softClose = useMutation({
    mutationFn: (name: string) =>
      postJson<Period>(`/api/sheets/${encodeURIComponent(sheet)}/periods/${encodeURIComponent(name)}/soft-close`, { user }),
    onMutate: () => setRefused(null),
    onError: (_e, name) => setRefused(name),
    // A close fixes the pool members' cuts for the period; a reopen makes them live again.
    onSettled: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: periodsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: poolsKey(sheet) }),
      ]),
  });

  const reopen = useMutation({
    mutationFn: (name: string) =>
      postJson<Period>(`/api/sheets/${encodeURIComponent(sheet)}/periods/${encodeURIComponent(name)}/reopen`, { user }),
    onMutate: () => setRefused(null),
    onError: (_e, name) => setRefused(name),
    // A close fixes the pool members' cuts for the period; a reopen makes them live again.
    onSettled: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: periodsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: poolsKey(sheet) }),
      ]),
  });

  const lock = useMutation({
    mutationFn: ({ name, action }: { name: string; action: 'lock' | 'unlock' }) =>
      postJson<Period>(`/api/sheets/${encodeURIComponent(sheet)}/periods/${encodeURIComponent(name)}/${action}`, { user }),
    onMutate: () => setRefused(null),
    onError: (_e, { name }) => setRefused(name),
    onSettled: () => queryClient.invalidateQueries({ queryKey: periodsKey(sheet) }),
  });

  // An error goes as soon as its form is changed or submitted again.
  const clearError = () => {
    save.reset();
    setInvalid(null);
  };

  // Refused here, before anything is sent, when the form alone shows it is bad.
  const check = (): string | null => {
    const n = name.trim();
    if (n === '') return 'A period needs a name.';
    if (!DATE.test(from.trim()) || !DATE.test(to.trim())) return 'The dates must be yyyy-mm-dd.';
    if (from.trim() > to.trim()) return 'The from-date must be on or before the to-date.';
    if (cap.trim() !== '' && !CAP.test(cap.trim())) return 'The cap must be a whole number of at least 1, or blank.';
    if (periods.some((p) => p.name === n)) return 'There is already a period of that name.';
    const clash = periods.find((p) => p.from <= to.trim() && from.trim() <= p.to);
    if (clash) return `The period overlaps ${clash.name}.`;
    return null;
  };

  const dueIn = (period: Period) => periodDue(period, all, people, rates, floor, late, today);

  return (
    <section data-testid="periods-panel">
      <h2>Periods</h2>
      <form
        data-testid="period-form"
        onSubmit={(e) => {
          e.preventDefault();
          const problem = check();
          if (problem !== null) {
            save.reset();
            setInvalid(problem);
            return;
          }
          setInvalid(null);
          submitOnce(() => save.mutateAsync({ name: name.trim(), from: from.trim(), to: to.trim(), cap: cap.trim() }));
        }}
      >
        <label>
          Name{' '}
          <input
            data-testid="period-form-name"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              clearError();
            }}
          />
        </label>{' '}
        <label>
          From{' '}
          <input
            data-testid="period-form-from"
            placeholder="yyyy-mm-dd"
            value={from}
            onChange={(e) => {
              setFrom(e.target.value);
              clearError();
            }}
          />
        </label>{' '}
        <label>
          To{' '}
          <input
            data-testid="period-form-to"
            placeholder="yyyy-mm-dd"
            value={to}
            onChange={(e) => {
              setTo(e.target.value);
              clearError();
            }}
          />
        </label>{' '}
        <label>
          Cap{' '}
          <input
            data-testid="period-form-cap"
            placeholder="none"
            inputMode="numeric"
            value={cap}
            onChange={(e) => {
              setCap(e.target.value);
              clearError();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="period-form-submit" disabled={save.isPending}>
          Add period
        </button>
        {(invalid !== null || save.isError) && (
          <span data-testid="period-form-error" role="alert">
            {' '}
            {invalid ?? 'Could not add this period.'}
          </span>
        )}
      </form>
      <form
        data-testid="period-generate-form"
        onSubmit={(e) => {
          e.preventDefault();
          clearGenerate();
          const f = genFrom.trim();
          const t = genTo.trim();
          if ((f !== '' && !DATE.test(f)) || (t !== '' && !DATE.test(t))) {
            setGenInvalid('The dates must be yyyy-mm-dd.');
            return;
          }
          if (f !== '' && t !== '' && f > t) {
            setGenInvalid('The to-date must not be before the from-date.');
            return;
          }
          generate.mutate({ kind: 'periods', from: f, to: t });
        }}
      >
        <label>
          From{' '}
          <input
            data-testid="period-generate-from"
            placeholder="yyyy-mm-dd"
            value={genFrom}
            onChange={(e) => {
              setGenFrom(e.target.value);
              clearGenerate();
            }}
          />
        </label>{' '}
        <label>
          To{' '}
          <input
            data-testid="period-generate-to"
            placeholder="yyyy-mm-dd"
            value={genTo}
            onChange={(e) => {
              setGenTo(e.target.value);
              clearGenerate();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="period-generate" disabled={generate.isPending}>
          Generate monthly periods
        </button>
        {generate.isSuccess && (
          <span>
            {' '}
            Made <strong data-testid="period-generated">{generate.data.generated}</strong>
          </span>
        )}
        {(genInvalid !== null || generate.isError) && (
          <span data-testid="period-generate-error" role="alert">
            {' '}
            {genInvalid ?? 'Could not generate these periods.'}
          </span>
        )}
      </form>
      <form
        data-testid="period-generate-fy-form"
        onSubmit={(e) => {
          e.preventDefault();
          clearGenerate();
          const y = fyYear.trim();
          if (!YEAR.test(y)) {
            setGenInvalid('The fiscal year must be a year, yyyy.');
            return;
          }
          generate.mutate({ kind: 'fiscal-year', year: y });
        }}
      >
        <label>
          Fiscal year{' '}
          <input
            data-testid="period-generate-fy-year"
            placeholder="yyyy"
            size={6}
            value={fyYear}
            onChange={(e) => {
              setFyYear(e.target.value);
              clearGenerate();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="period-generate-fy" disabled={generate.isPending}>
          Generate fiscal year
        </button>
      </form>
      <form
        data-testid="period-generate-quarters-form"
        onSubmit={(e) => {
          e.preventDefault();
          clearGenerate();
          const y = qYear.trim();
          if (!YEAR.test(y)) {
            setGenInvalid('The year must be yyyy.');
            return;
          }
          generate.mutate({ kind: 'quarters', year: y });
        }}
      >
        <label>
          Year{' '}
          <input
            data-testid="period-generate-quarters-year"
            placeholder="yyyy"
            size={6}
            value={qYear}
            onChange={(e) => {
              setQYear(e.target.value);
              clearGenerate();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="period-generate-quarters" disabled={generate.isPending}>
          Generate quarters
        </button>
      </form>
      <table data-testid="period-list">
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
          {periods.map((p) => (
            <tr key={p.name} data-testid={`period-row-${p.name}`}>
              <td data-testid="period-name">{p.name}</td>
              <td data-testid="period-from">{p.from}</td>
              <td data-testid="period-to">{p.to}</td>
              <td data-testid="period-due-total">{dueIn(p)}</td>
              <td data-testid="period-closed">{p.closed ? (p.soft ? 'soft' : 'yes') : 'no'}</td>
              <td data-testid="period-fy">{fiscalYear(p.from, fiscalStart)}</td>
              <td data-testid="period-cap">{p.cap ?? ''}</td>
              <td data-testid="period-locked">{p.locked ? 'yes' : 'no'}</td>
              <td>
                <button
                  type="button"
                  data-testid="period-close"
                  disabled={close.isPending}
                  onClick={() => close.mutate(p.name)}
                >
                  Close
                </button>{' '}
                <button
                  type="button"
                  data-testid="period-soft-close"
                  disabled={softClose.isPending}
                  onClick={() => softClose.mutate(p.name)}
                >
                  Soft close
                </button>{' '}
                <button
                  type="button"
                  data-testid="period-reopen"
                  disabled={reopen.isPending}
                  onClick={() => reopen.mutate(p.name)}
                >
                  Reopen
                </button>{' '}
                <button
                  type="button"
                  data-testid="period-lock"
                  disabled={lock.isPending}
                  onClick={() => lock.mutate({ name: p.name, action: 'lock' })}
                >
                  Lock
                </button>{' '}
                <button
                  type="button"
                  data-testid="period-unlock"
                  disabled={lock.isPending}
                  onClick={() => lock.mutate({ name: p.name, action: 'unlock' })}
                >
                  Unlock
                </button>
                {refused === p.name && (
                  <span data-testid="period-error" role="alert">
                    {' '}
                    Could not change this period.
                  </span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 6, Component: PeriodsPanel });
