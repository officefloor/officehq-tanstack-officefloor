import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import { useUser } from '../session/session';
import { useRates } from '../rates/rates';
import { useFloor, useLate } from '../access/access';
import { periodClaimCount, periodDueTotal, periodsKey, usePeriods } from './periods';
import type { Period } from './periods';

// A cap, when given, is the most OPEN or PENDING claims a person may have in the period.
type Fields = { name: string; from: string; to: string; cap: string };
const EMPTY: Fields = { name: '', from: '', to: '', cap: '' };

// The sheet's periods: a form that adds one (a from-date left blank is the sheet's today), and each
// period with its due-total: the due of the claims dated in it that counted at its end (by their status log), fixed when the
// period is closed. A closed period's claims cannot be changed. A soft-closed period's figures are
// fixed as a close fixes them, but its claims can still be changed; it can be closed (its figures
// fixed afresh) or reopened. A steward may reopen a closed or soft-closed period, and may lock a period
// (it cannot then be reopened) and unlock it.
function Periods({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const periods = usePeriods(sheetId);
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const rates = useRates(sheetId).data ?? [];
  const [fields, setFields] = useState<Fields>(EMPTY);
  const [error, setError] = useState('');
  const [user] = useUser();

  const [refused, setRefused] = useState<{ name: string; action: 'close' | 'soft-close' | 'reopen' | 'lock' | 'unlock' } | null>(null);

  const once = useOnce();
  const close = useMutation({
    mutationFn: (name: string) =>
      postJson<Period>(
        `/api/sheets/${encodeURIComponent(sheetId)}/periods/${encodeURIComponent(name)}/close`,
        {},
        user,
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
    onError: (_e, name) => setRefused({ name, action: 'close' }),
  });

  const closePeriod = (name: string) => {
    setRefused(null);
    once((done) => close.mutate(name, { onSettled: done }));
  };

  const softClose = useMutation({
    mutationFn: (name: string) =>
      postJson<Period>(
        `/api/sheets/${encodeURIComponent(sheetId)}/periods/${encodeURIComponent(name)}/soft-close`,
        {},
        user,
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
    onError: (_e, name) => setRefused({ name, action: 'soft-close' }),
  });

  const softClosePeriod = (name: string) => {
    setRefused(null);
    once((done) => softClose.mutate(name, { onSettled: done }));
  };

  // Only a steward may reopen a closed period; its due-total is then live again.
  const reopen = useMutation({
    mutationFn: (name: string) =>
      postJson<Period>(
        `/api/sheets/${encodeURIComponent(sheetId)}/periods/${encodeURIComponent(name)}/reopen`,
        {},
        user,
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
    onError: (_e, name) => setRefused({ name, action: 'reopen' }),
  });

  const reopenPeriod = (name: string) => {
    setRefused(null);
    once((done) => reopen.mutate(name, { onSettled: done }));
  };

  // Only a steward may lock or unlock a period; a locked period cannot be reopened.
  const lock = useMutation({
    mutationFn: ({ name, action }: { name: string; action: 'lock' | 'unlock' }) =>
      postJson<Period>(
        `/api/sheets/${encodeURIComponent(sheetId)}/periods/${encodeURIComponent(name)}/${action}`,
        {},
        user,
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
    onError: (_e, { name, action }) => setRefused({ name, action }),
  });

  const lockPeriod = (name: string, action: 'lock' | 'unlock') => {
    setRefused(null);
    once((done) => lock.mutate({ name, action }, { onSettled: done }));
  };

  const save = useMutation({
    mutationFn: (body: Fields) =>
      postJson<Period>(`/api/sheets/${encodeURIComponent(sheetId)}/periods`, body, user),
    onSuccess: () => {
      setFields(EMPTY);
      void queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
    onError: () => setError('That period could not be added.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const body = { name: fields.name.trim(), from: fields.from.trim(), to: fields.to.trim(), cap: fields.cap.trim() };
    if (body.name === '') {
      setError('A name is required.');
      return;
    }
    if (body.to === '') {
      setError('A to-date is required.');
      return;
    }
    if (body.cap !== '' && !/^[1-9]\d*$/.test(body.cap)) {
      setError('A cap is a whole number, at least 1.');
      return;
    }
    once((done) => save.mutate(body, { onSettled: done }));
  };

  // Generate one period per calendar month between two dates (a from-date left blank is the sheet's
  // today); months already named or overlapping a period are left out. Shows how many were made,
  // the count cleared while it runs.
  const [gen, setGen] = useState({ from: '', to: '' });
  const [made, setMade] = useState<number | null>(null);
  const [genError, setGenError] = useState('');

  const generate = useMutation({
    mutationFn: (body: { from: string; to: string }) =>
      postJson<{ made: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/periods/generate`, body, user),
    onSuccess: (r) => {
      setMade(r.made);
      void queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
    onError: () => setGenError('Those periods could not be generated.'),
  });

  const submitGenerate = (e: FormEvent) => {
    e.preventDefault();
    setMade(null);
    setGenError('');
    const body = { from: gen.from.trim(), to: gen.to.trim() };
    if (body.to === '') {
      setGenError('A to-date is required.');
      return;
    }
    once((done) => generate.mutate(body, { onSettled: done }));
  };

  // Generate a fiscal year's twelve monthly periods, as above, from the first day of the sheet's
  // fiscal start month in the year typed. Shares the made count and error with the generator above.
  const [fyYear, setFyYear] = useState('');

  const generateFy = useMutation({
    mutationFn: (body: { year: string }) =>
      postJson<{ made: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/periods/generate-fy`, body, user),
    onSuccess: (r) => {
      setMade(r.made);
      void queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
    onError: () => setGenError('That fiscal year could not be generated.'),
  });

  const submitGenerateFy = (e: FormEvent) => {
    e.preventDefault();
    setMade(null);
    setGenError('');
    const year = fyYear.trim();
    if (!/^\d{4}$/.test(year)) {
      setGenError('A fiscal year is a year, yyyy.');
      return;
    }
    once((done) => generateFy.mutate({ year }, { onSettled: done }));
  };

  // Roll a fiscal year over (a steward only): the next fiscal year's twelve periods are generated as
  // above, and each person's open figure at the old year's end is carried into the new year's first
  // period as an opening balance. Its own made count and error.
  const [rolloverYear, setRolloverYear] = useState('');
  const [rolloverMade, setRolloverMade] = useState<number | null>(null);
  const [rolloverError, setRolloverError] = useState('');

  const rollover = useMutation({
    mutationFn: (body: { year: string }) =>
      postJson<{ made: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/periods/rollover`, body, user),
    onSuccess: (r) => {
      setRolloverMade(r.made);
      void queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
    onError: () => setRolloverError('That fiscal year could not be rolled over.'),
  });

  const submitRollover = (e: FormEvent) => {
    e.preventDefault();
    setRolloverMade(null);
    setRolloverError('');
    const year = rolloverYear.trim();
    if (!/^\d{4}$/.test(year)) {
      setRolloverError('A fiscal year is a year, yyyy.');
      return;
    }
    once((done) => rollover.mutate({ year }, { onSettled: done }));
  };

  // Undo the last rollover (the steward who made it, on the same sheet-today): its generated periods
  // are removed and its opening balances taken back. Shares the rollover's error.
  const rolloverUndo = useMutation({
    mutationFn: () =>
      postJson<{ made: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/periods/rollover-undo`, {}, user),
    onSuccess: () => {
      void queryClient.invalidateQueries();
    },
    onError: () => setRolloverError('The last rollover could not be undone.'),
  });

  const undoRollover = () => {
    setRolloverMade(null);
    setRolloverError('');
    once((done) => rolloverUndo.mutate(undefined, { onSettled: done }));
  };

  // Generate a calendar year's four quarters, yyyy-Q1 to yyyy-Q4, each left out when its name exists
  // or its days overlap a period. Shares the made count and error with the generators above.
  const [quarterYear, setQuarterYear] = useState('');

  const generateQuarters = useMutation({
    mutationFn: (body: { year: string }) =>
      postJson<{ made: number }>(`/api/sheets/${encodeURIComponent(sheetId)}/periods/generate-quarters`, body, user),
    onSuccess: (r) => {
      setMade(r.made);
      void queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
    onError: () => setGenError('Those quarters could not be generated.'),
  });

  const submitGenerateQuarters = (e: FormEvent) => {
    e.preventDefault();
    setMade(null);
    setGenError('');
    const year = quarterYear.trim();
    if (!/^\d{4}$/.test(year)) {
      setGenError('A year is yyyy.');
      return;
    }
    once((done) => generateQuarters.mutate({ year }, { onSettled: done }));
  };

  const changeGenerate = (next: Partial<{ from: string; to: string }>) => {
    setGen((g) => ({ ...g, ...next }));
    setGenError('');
  };

  const change = (next: Partial<Fields>) => {
    setFields((f) => ({ ...f, ...next }));
    setError('');
  };


  return (
    <section data-testid="periods">
      <h2>Periods</h2>
      <form onSubmit={submit} data-testid="period-form">
        <label>
          Name{' '}
          <input data-testid="period-form-name" value={fields.name} onChange={(e) => change({ name: e.target.value })} />
        </label>{' '}
        <label>
          From{' '}
          <input
            type="date"
            data-testid="period-form-from"
            value={fields.from}
            onChange={(e) => change({ from: e.target.value })}
          />
        </label>{' '}
        <label>
          To{' '}
          <input
            type="date"
            data-testid="period-form-to"
            value={fields.to}
            onChange={(e) => change({ to: e.target.value })}
          />
        </label>{' '}
        <label>
          Cap{' '}
          <input data-testid="period-form-cap" value={fields.cap} onChange={(e) => change({ cap: e.target.value })} />
        </label>{' '}
        <button type="submit" data-testid="period-form-submit" disabled={save.isPending}>
          Add period
        </button>
        {error && (
          <p role="alert" data-testid="period-form-error">
            {error}
          </p>
        )}
      </form>
      <form onSubmit={submitGenerate} data-testid="period-generate-form">
        <label>
          From{' '}
          <input
            type="date"
            data-testid="period-generate-from"
            value={gen.from}
            onChange={(e) => changeGenerate({ from: e.target.value })}
          />
        </label>{' '}
        <label>
          To{' '}
          <input
            type="date"
            data-testid="period-generate-to"
            value={gen.to}
            onChange={(e) => changeGenerate({ to: e.target.value })}
          />
        </label>{' '}
        <button type="submit" data-testid="period-generate" disabled={generate.isPending}>
          Generate monthly periods
        </button>
      </form>
      <form onSubmit={submitGenerateFy} data-testid="period-generate-fy-form">
        <label>
          Fiscal year FY{' '}
          <input
            data-testid="period-generate-fy-year"
            inputMode="numeric"
            placeholder="yyyy"
            value={fyYear}
            onChange={(e) => {
              setFyYear(e.target.value);
              setGenError('');
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="period-generate-fy" disabled={generateFy.isPending}>
          Generate fiscal year
        </button>{' '}
      </form>
      <form onSubmit={submitGenerateQuarters} data-testid="period-generate-quarters-form">
        <label>
          Year{' '}
          <input
            data-testid="period-generate-quarters-year"
            inputMode="numeric"
            placeholder="yyyy"
            value={quarterYear}
            onChange={(e) => {
              setQuarterYear(e.target.value);
              setGenError('');
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="period-generate-quarters" disabled={generateQuarters.isPending}>
          Generate quarters
        </button>{' '}
        {made !== null && (
          <>
            {' '}Made <span data-testid="period-generated">{made}</span>
          </>
        )}
        {genError && (
          <p role="alert" data-testid="period-generate-error">
            {genError}
          </p>
        )}
      </form>
      <form onSubmit={submitRollover} data-testid="rollover-form">
        <label>
          Roll over FY{' '}
          <input
            data-testid="rollover-year"
            inputMode="numeric"
            placeholder="yyyy"
            value={rolloverYear}
            onChange={(e) => {
              setRolloverYear(e.target.value);
              setRolloverError('');
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="rollover" disabled={rollover.isPending}>
          Roll year over
        </button>
        <button type="button" data-testid="rollover-undo" disabled={rolloverUndo.isPending} onClick={undoRollover}>
          Undo last rollover
        </button>
        {rolloverMade !== null && (
          <>
            {' '}Made <span data-testid="rollover-made">{rolloverMade}</span>
          </>
        )}
        {rolloverError && (
          <p role="alert" data-testid="rollover-error">
            {rolloverError}
          </p>
        )}
      </form>
      {periods.isError && (
        <p role="alert" data-testid="periods-error">
          The periods could not be read.
        </p>
      )}
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
            <th>Claims</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {(periods.data ?? []).map((p) => (
            <tr key={p.name} data-testid={`period-row-${p.name}`}>
              <td data-testid="period-name">{p.name}</td>
              <td data-testid="period-from">{p.from}</td>
              <td data-testid="period-to">{p.to}</td>
              <td data-testid="period-due-total">{periodDueTotal(p, people, claims, rates, floor, late)}</td>
              <td data-testid="period-closed">{p.closed ? (p.soft ? 'soft' : 'yes') : 'no'}</td>
              <td data-testid="period-fy">{p.fy}</td>
              <td data-testid="period-cap">{p.cap}</td>
              <td data-testid="period-locked">{p.locked ? 'yes' : 'no'}</td>
              <td data-testid="period-claims-count">{periodClaimCount(p, claims)}</td>
              <td>
                <button
                  type="button"
                  data-testid="period-close"
                  disabled={close.isPending}
                  onClick={() => closePeriod(p.name)}
                >
                  Close
                </button>{' '}
                <button
                  type="button"
                  data-testid="period-soft-close"
                  disabled={softClose.isPending}
                  onClick={() => softClosePeriod(p.name)}
                >
                  Soft close
                </button>{' '}
                <button
                  type="button"
                  data-testid="period-reopen"
                  disabled={reopen.isPending}
                  onClick={() => reopenPeriod(p.name)}
                >
                  Reopen
                </button>{' '}
                <button
                  type="button"
                  data-testid="period-lock"
                  disabled={lock.isPending}
                  onClick={() => lockPeriod(p.name, 'lock')}
                >
                  Lock
                </button>{' '}
                <button
                  type="button"
                  data-testid="period-unlock"
                  disabled={lock.isPending}
                  onClick={() => lockPeriod(p.name, 'unlock')}
                >
                  Unlock
                </button>
                {refused?.name === p.name && (
                  <p role="alert" data-testid="period-error">
                    {refused.action === 'close'
                      ? 'That period could not be closed.'
                      : refused.action === 'soft-close'
                        ? 'That period could not be soft-closed.'
                        : refused.action === 'reopen'
                          ? 'That period could not be reopened.'
                          : refused.action === 'lock'
                            ? 'That period could not be locked.'
                            : 'That period could not be unlocked.'}
                  </p>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: -5, Component: Periods });
