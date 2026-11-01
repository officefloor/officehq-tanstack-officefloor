import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { peoplePath } from '../people/people';
import type { Person } from '../people/people';
import { claimed, claimsKey, claimsPath, useClaims } from './claims';
import type { Claim } from './claims';

type Draft = { amount: string; date: string; note: string };
const EMPTY: Draft = { amount: '', date: '', note: '' };

function ClaimedHeader() {
  return (
    <>
      <th>Claimed</th>
      <th>Claims</th>
    </>
  );
}

// A person's claimed total, then their claims (newest first) under one form that adds a claim or,
// once a claim's editor is opened, edits it. A blank date takes the sheet's today on the server.
function PersonClaims({ sheet, person }: { sheet: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const { data: all = [] } = useClaims(sheet);
  const claims = all.filter((c) => c.person === person.id);
  const [draft, setDraft] = useState<Draft>(EMPTY);
  const [editing, setEditing] = useState<number | null>(null);

  const save = useMutation({
    mutationFn: ({ id, body }: { id: number | null; body: Draft }) =>
      id === null
        ? postJson<Claim[]>(`${peoplePath(sheet)}/${person.id}/claims`, body)
        : putJson<Claim[]>(`${claimsPath(sheet)}/${id}`, body),
    onSuccess: (updated) => {
      queryClient.setQueryData(claimsKey(sheet), updated);
      setDraft(EMPTY);
      setEditing(null);
    },
  });

  const change = (next: Partial<Draft>) => {
    setDraft((d) => ({ ...d, ...next }));
    save.reset();
  };

  const open = (c: Claim) => {
    setEditing(c.id);
    setDraft({ amount: c.amount, date: c.date, note: c.note });
    save.reset();
  };

  const cancel = () => {
    setEditing(null);
    setDraft(EMPTY);
    save.reset();
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    save.reset();
    // The server refuses an amount that is not 0.00 or more with up to two decimals, or a bad date.
    save.mutate({ id: editing, body: draft });
  };

  return (
    <>
      <td data-testid="person-claimed">{claimed(claims)}</td>
      <td>
        <form onSubmit={submit}>
          <input
            data-testid="claim-form-amount"
            placeholder="amount"
            value={draft.amount}
            onChange={(e) => change({ amount: e.target.value })}
          />{' '}
          <input
            data-testid="claim-form-date"
            placeholder="yyyy-mm-dd"
            value={draft.date}
            onChange={(e) => change({ date: e.target.value })}
          />{' '}
          <input
            data-testid="claim-form-note"
            placeholder="note"
            value={draft.note}
            onChange={(e) => change({ note: e.target.value })}
          />{' '}
          <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
            {editing === null ? 'Add claim' : 'Save claim'}
          </button>
          {save.isError && (
            <span data-testid="claim-form-error" role="alert">
              {' '}A claim needs an amount of 0.00 or more with up to two decimals, and a valid date.
            </span>
          )}
        </form>
        <ul>
          {claims.map((c) => (
            <li key={c.id} data-testid={`claim-row-${c.id}`}>
              <span data-testid="claim-amount">{c.amount}</span>{' '}
              <span data-testid="claim-date">{c.date}</span>{' '}
              <span data-testid="claim-note">{c.note}</span>{' '}
              <button type="button" data-testid="claim-edit" onClick={() => open(c)}>
                Edit
              </button>
              {editing === c.id && (
                <button type="button" data-testid="claim-cancel" onClick={cancel}>
                  Cancel
                </button>
              )}
            </li>
          ))}
        </ul>
      </td>
    </>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 10, Component: ClaimedHeader }),
  PersonColumns.fill({ order: 10, Component: PersonClaims }),
];
