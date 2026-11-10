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
import { periodDueTotal, periodsKey, usePeriods } from './periods';
import type { Period } from './periods';

type Fields = { name: string; from: string; to: string };
const EMPTY: Fields = { name: '', from: '', to: '' };

// The sheet's periods: a form that adds one (a from-date left blank is the sheet's today), and each
// period with its due-total: the due of the claims dated in it that counted at its end (by their status log), fixed when the
// period is closed. A closed period's claims cannot be changed. A soft-closed period's figures are
// fixed as a close fixes them, but its claims can still be changed; it can be closed (its figures
// fixed afresh) or reopened. A steward may reopen a closed or soft-closed period.
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

  const [refused, setRefused] = useState<{ name: string; action: 'close' | 'soft-close' | 'reopen' } | null>(null);

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
    const body = { name: fields.name.trim(), from: fields.from.trim(), to: fields.to.trim() };
    if (body.name === '') {
      setError('A name is required.');
      return;
    }
    if (body.to === '') {
      setError('A to-date is required.');
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
                </button>
                {refused?.name === p.name && (
                  <p role="alert" data-testid="period-error">
                    {refused.action === 'close'
                      ? 'That period could not be closed.'
                      : refused.action === 'soft-close'
                        ? 'That period could not be soft-closed.'
                        : 'That period could not be reopened.'}
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
