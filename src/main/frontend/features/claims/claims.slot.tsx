import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import type { Person } from '../people/people';
import { useUser } from '../session/session';
import { AMOUNT, claimsKey, sumAmounts, useClaims } from './claims';
import type { Claim } from './claims';
import { claimDue, rateOn, useRates } from '../rates/rates';

type Action = 'open' | 'settle' | 'void';
const ACTIONS: { action: Action; label: string }[] = [
  { action: 'open', label: 'Open' },
  { action: 'settle', label: 'Settle' },
  { action: 'void', label: 'Void' },
];

type Fields = { amount: string; date: string; note: string };
const EMPTY: Fields = { amount: '', date: '', note: '' };

function ClaimsHead() {
  return (
    <>
      <th>Claimed</th>
      <th>Claims</th>
    </>
  );
}

// A person's claims: what they have claimed in all, a form that adds a claim (or edits the one
// whose editor is open), and their claims, newest first.
function PersonClaims({ sheetId, person }: { sheetId: string; person: Person }) {
  const queryClient = useQueryClient();
  const all = useClaims(sheetId);
  const claims = (all.data ?? []).filter((c) => c.personId === person.id);
  const [fields, setFields] = useState<Fields>(EMPTY);
  const [editing, setEditing] = useState<string | null>(null);
  const [error, setError] = useState('');
  // The claim whose last status change was refused: its error shows until its next action.
  const [refused, setRefused] = useState<string | null>(null);
  const [user] = useUser();
  const rates = useRates(sheetId).data ?? [];

  const reset = () => {
    setFields(EMPTY);
    setEditing(null);
    setError('');
  };

  const save = useMutation({
    mutationFn: ({ id, body }: { id: string | null; body: Fields }) => {
      const sheet = `/api/sheets/${encodeURIComponent(sheetId)}`;
      return id === null
        ? postJson<Claim>(`${sheet}/people/${encodeURIComponent(person.id)}/claims`, body, user)
        : putJson<Claim>(`${sheet}/claims/${encodeURIComponent(id)}`, body, user);
    },
    onSuccess: () => {
      reset();
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: () => setError('That claim could not be saved.'),
  });

  const move = useMutation({
    mutationFn: ({ id, action }: { id: string; action: Action }) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheetId)}/claims/${encodeURIComponent(id)}/${action}`,
        {},
        user,
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
    onError: (_e, { id }) => setRefused(id),
  });

  const act = (id: string, action: Action) => {
    setRefused(null);
    move.mutate({ id, action });
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const amount = fields.amount.trim();
    if (amount === '') {
      setError('An amount is required.');
      return;
    }
    if (!AMOUNT.test(amount)) {
      setError('An amount is a number of 0.00 or more, with up to two decimals.');
      return;
    }
    save.mutate({ id: editing, body: { ...fields, amount } });
  };

  const change = (next: Partial<Fields>) => {
    setFields((f) => ({ ...f, ...next }));
    setError('');
  };

  const open = (c: Claim) => {
    setRefused(null);
    setEditing(c.id);
    setFields({ amount: c.amount, date: c.date, note: c.note });
    setError('');
  };

  return (
    <>
      <td data-testid="person-claimed">{sumAmounts(claims)}</td>
      <td>
        <form onSubmit={submit} data-testid="claim-form">
          <input
            data-testid="claim-form-amount"
            placeholder="Amount"
            inputMode="decimal"
            value={fields.amount}
            onChange={(e) => change({ amount: e.target.value })}
          />{' '}
          <input
            type="date"
            data-testid="claim-form-date"
            value={fields.date}
            onChange={(e) => change({ date: e.target.value })}
          />{' '}
          <input
            data-testid="claim-form-note"
            placeholder="Note"
            value={fields.note}
            onChange={(e) => change({ note: e.target.value })}
          />{' '}
          <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
            {editing === null ? 'Add claim' : 'Save claim'}
          </button>
          {error && (
            <p role="alert" data-testid="claim-form-error">
              {error}
            </p>
          )}
        </form>
        {all.isError && (
          <p role="alert" data-testid="claims-error">
            The claims could not be read.
          </p>
        )}
        <ul data-testid="claim-list">
          {claims.map((c) => (
            <li key={c.id} data-testid={`claim-row-${c.id}`}>
              <span data-testid="claim-number">{c.number}</span>{' '}
              <span data-testid="claim-amount">{c.amount}</span>{' '}
              <span data-testid="claim-date">{c.date}</span>{' '}
              <span data-testid="claim-note">{c.note}</span>{' '}
              <span data-testid="claim-due">{claimDue(c, rateOn(rates, person.grade, c.date))}</span>{' '}
              <span data-testid="claim-status">{c.status}</span>{' '}
              <span data-testid="claim-opened-by">{c.openedBy}</span>{' '}
              {ACTIONS.map(({ action, label }) => (
                <button
                  key={action}
                  type="button"
                  data-testid={`claim-${action}`}
                  onClick={() => act(c.id, action)}
                >
                  {label}
                </button>
              ))}{' '}
              {editing === c.id ? (
                <button type="button" data-testid="claim-cancel" onClick={reset}>
                  Cancel
                </button>
              ) : (
                <button type="button" data-testid="claim-edit" onClick={() => open(c)}>
                  Edit
                </button>
              )}
              {refused === c.id && (
                <p role="alert" data-testid="claim-error">
                  That status change is not allowed.
                </p>
              )}
            </li>
          ))}
        </ul>
      </td>
    </>
  );
}

export const contributions = [
  PersonHead.fill({ order: 10, Component: ClaimsHead }),
  PersonRow.fill({ order: 10, Component: PersonClaims }),
];
