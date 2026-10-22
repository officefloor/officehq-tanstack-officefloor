import { useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import { useFloor, useLate } from '../settings/settings';
import { useRates } from '../rates/rates';
import { periodDueTotal, periodsKey, usePeriods } from './periods';
import type { Period } from './periods';

const DATE = /^\d{4}-\d{2}-\d{2}$/;

/** One period with its due-total (see periodDueTotal), whether it is closed, and the buttons that close it and
 *  (a steward only) reopen it. */
function PeriodRow({ sheetId, user, period, dueTotal }: { sheetId: string; user: string; period: Period; dueTotal: string }) {
  const queryClient = useQueryClient();
  const [error, setError] = useState('');

  const close = useMutation({
    mutationFn: () =>
      putJson<Period>(`/api/sheets/${encodeURIComponent(sheetId)}/periods/${encodeURIComponent(period.name)}/close`, { user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : 'Could not close this period.'),
  });

  const reopen = useMutation({
    mutationFn: () =>
      putJson<Period>(`/api/sheets/${encodeURIComponent(sheetId)}/periods/${encodeURIComponent(period.name)}/reopen`, { user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : 'Could not reopen this period.'),
  });

  return (
    <tr data-testid={`period-row-${period.name}`}>
      <td data-testid="period-name">{period.name}</td>
      <td data-testid="period-from">{period.from}</td>
      <td data-testid="period-to">{period.to}</td>
      <td>
        Due total <span data-testid="period-due-total">{dueTotal}</span>
      </td>
      <td>
        Closed <span data-testid="period-closed">{period.closed ? 'yes' : 'no'}</span>
      </td>
      <td>
        <button
          type="button"
          data-testid="period-close"
          disabled={close.isPending}
          onClick={() => {
            setError('');
            close.mutate();
          }}
        >
          Close
        </button>{' '}
        <button
          type="button"
          data-testid="period-reopen"
          disabled={reopen.isPending}
          onClick={() => {
            setError('');
            reopen.mutate();
          }}
        >
          Reopen
        </button>
        {error && (
          <p data-testid="period-error" role="alert">
            {error}
          </p>
        )}
      </td>
    </tr>
  );
}

/** The sheet's periods, each with its due-total, and the form that adds one. */
function Periods({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const periods = usePeriods(sheetId).data ?? [];
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const [name, setName] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const add = useMutation({
    mutationFn: (body: { name: string; from: string; to: string }) =>
      postJson<Period>(`/api/sheets/${encodeURIComponent(sheetId)}/periods`, { user, ...body }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
      setName('');
      setFrom('');
      setTo('');
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body ? e.body : 'Could not add this period.'),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (name.trim() === '') {
      setError('The period needs a name.');
      return;
    }
    if (!DATE.test(from.trim()) || !DATE.test(to.trim())) {
      setError('The from and to dates must be dates, yyyy-mm-dd.');
      return;
    }
    if (from.trim() > to.trim()) {
      setError('The from-date must be on or before the to-date.');
      return;
    }
    if (!once.start()) return;
    add.mutate({ name: name.trim(), from: from.trim(), to: to.trim() });
  };

  const change = (set: (v: string) => void) => (e: ChangeEvent<HTMLInputElement>) => {
    set(e.target.value);
    setError('');
  };

  return (
    <section data-testid="periods">
      <h2>Periods</h2>
      <form data-testid="period-form" onSubmit={submit}>
        <label>
          Name <input data-testid="period-form-name" value={name} onChange={change(setName)} />
        </label>{' '}
        <label>
          From <input data-testid="period-form-from" placeholder="yyyy-mm-dd" value={from} onChange={change(setFrom)} />
        </label>{' '}
        <label>
          To <input data-testid="period-form-to" placeholder="yyyy-mm-dd" value={to} onChange={change(setTo)} />
        </label>{' '}
        <button type="submit" data-testid="period-form-submit" disabled={add.isPending}>
          Add period
        </button>
        {error && (
          <p data-testid="period-form-error" role="alert">
            {error}
          </p>
        )}
      </form>
      <table>
        <tbody>
          {periods.map((p) => (
            <PeriodRow key={p.name} sheetId={sheetId} user={user} period={p} dueTotal={periodDueTotal(p, claims, people, rates, floor, late)} />
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 6, Component: Periods });
