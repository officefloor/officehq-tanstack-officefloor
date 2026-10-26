import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { ClaimCells } from '../../slots/defs/claimColumns';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsQuery } from '../claims/claimsQuery';
import type { Claim } from '../claims/claimsQuery';
import { peopleQuery } from '../people/peopleQuery';
import { ratesQuery } from '../rates/ratesQuery';
import { useFloor, useLate } from '../settings/settingsQuery';
import { useUser } from '../session/useUser';
import { periodDue, periodOf, periodsKey, periodsQuery } from './periodsQuery';
import type { Period } from './periodsQuery';

const DATE = /^\d{4}-\d{2}-\d{2}$/;

function ClaimPeriod({ sheetId, claim }: { sheetId: string; claim: Claim }) {
  const periods = useQuery(periodsQuery(sheetId)).data ?? [];
  return <td data-testid="claim-period">{periodOf(periods, claim.date)?.name ?? ''}</td>;
}

/** A period's closed flag and the buttons that close and reopen it (reopening is a steward's). A refusal
 *  shows on the period until its next action. */
function PeriodClosed({ sheetId, period }: { sheetId: string; period: Period }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [error, setError] = useState('');
  const once = useSubmitOnce();
  const close = useMutation({
    mutationFn: () =>
      putJson<Period>(
        `/api/sheets/${encodeURIComponent(sheetId)}/periods/${encodeURIComponent(period.name)}/close`,
        {},
        user,
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) }),
    onError: () => setError(`The period ${period.name} could not be closed: it is already closed.`),
  });
  const reopen = useMutation({
    mutationFn: () =>
      putJson<Period>(
        `/api/sheets/${encodeURIComponent(sheetId)}/periods/${encodeURIComponent(period.name)}/reopen`,
        {},
        user,
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) }),
    onError: () =>
      setError(`The period ${period.name} could not be reopened: it is not closed, or you are not a steward.`),
  });

  return (
    <td>
      <span data-testid="period-closed">{period.closed ? 'yes' : 'no'}</span>{' '}
      <button
        type="button"
        data-testid="period-close"
        disabled={close.isPending}
        onClick={() => {
          setError('');
          once(() => close.mutateAsync());
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
          once(() => reopen.mutateAsync());
        }}
      >
        Reopen
      </button>
      {error && <span data-testid="period-error">{error}</span>}
    </td>
  );
}

/** The sheet's periods, each with its due-total — the figures of the counting claims dated in it, fixed
 *  while the period is closed and live again once it is reopened — and the form a period is added through. */
function Periods({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const periods = useQuery(periodsQuery(sheetId));
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const rates = useQuery(ratesQuery(sheetId)).data ?? [];
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const [name, setName] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [error, setError] = useState('');

  const [user] = useUser();
  const once = useSubmitOnce();
  const add = useMutation({
    mutationFn: (body: Pick<Period, 'name' | 'from' | 'to'>) =>
      postJson<Period>(`/api/sheets/${encodeURIComponent(sheetId)}/periods`, body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: periodsKey(sheetId) });
      setName('');
      setFrom('');
      setTo('');
    },
    onError: () => setError('The period could not be added: its name must be new and its dates must not overlap another period.'),
  });

  const list = periods.data ?? [];
  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const n = name.trim();
    const f = from.trim();
    const t = to.trim();
    if (n === '') {
      setError('A period needs a name.');
      return;
    }
    if (!DATE.test(f) || !DATE.test(t)) {
      setError('The from and to dates must be dates (yyyy-mm-dd).');
      return;
    }
    if (t < f) {
      setError('The to-date must be on or after the from-date.');
      return;
    }
    if (list.some((p) => p.name === n)) {
      setError(`There is already a period named ${n}.`);
      return;
    }
    const overlapped = list.find((p) => p.from <= t && f <= p.to);
    if (overlapped) {
      setError(`The period overlaps ${overlapped.name}.`);
      return;
    }
    once(() => add.mutateAsync({ name: n, from: f, to: t }));
  };

  const dueTotal = (period: Period) => periodDue(period, claims, people, rates, floor, late);

  const edit = (set: (v: string) => void) => (e: { target: { value: string } }) => {
    set(e.target.value);
    setError('');
  };

  return (
    <section data-testid="periods">
      <h3>Periods</h3>
      <form data-testid="period-form" onSubmit={submit}>
        <label>
          Name <input data-testid="period-form-name" value={name} onChange={edit(setName)} />
        </label>{' '}
        <label>
          From <input data-testid="period-form-from" type="date" value={from} onChange={edit(setFrom)} />
        </label>{' '}
        <label>
          To <input data-testid="period-form-to" type="date" value={to} onChange={edit(setTo)} />
        </label>{' '}
        <button type="submit" data-testid="period-form-submit" disabled={add.isPending}>
          Add period
        </button>
        {error && <p data-testid="period-form-error">{error}</p>}
      </form>
      {periods.isError && <p data-testid="periods-load-error">Could not load the periods.</p>}
      <table data-testid="periods-list">
        <thead>
          <tr>
            <th>Period</th>
            <th>From</th>
            <th>To</th>
            <th>Due</th>
            <th>Closed</th>
          </tr>
        </thead>
        <tbody>
          {list.map((p) => (
            <tr key={p.name} data-testid={`period-row-${p.name}`}>
              <td data-testid="period-name">{p.name}</td>
              <td data-testid="period-from">{p.from}</td>
              <td data-testid="period-to">{p.to}</td>
              <td data-testid="period-due-total">{dueTotal(p)}</td>
              <PeriodClosed sheetId={sheetId} period={p} />
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

// Keyed by sheet, so a typed period or an error never carries over to another sheet.
export const contributions = [
  ClaimCells.fill({ order: 15, Component: ClaimPeriod }),
  SheetPanel.fill({ order: 7, Component: ({ sheetId }) => <Periods key={sheetId} sheetId={sheetId} /> }),
];
