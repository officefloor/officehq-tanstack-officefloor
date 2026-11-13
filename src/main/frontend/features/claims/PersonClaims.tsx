import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { ClaimRowValues } from '../../slots/defs/claimRow';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import type { Person } from '../people/people';
import { SIGNED_AMOUNT, claimsKey, useClaims } from './claims';
import type { Claim, ClaimInput } from './claims';
import { useOneAtATime } from '../../query/useOneAtATime';

const EMPTY: ClaimInput = { amount: '', date: '', note: '', corrects: '' };

// A person's claims, newest first, and the form that adds one — or, while a claim's editor is
// open, saves that claim's changes. Opening an editor fills the form with the claim's values.
export function PersonClaims({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const claims = (useClaims().data ?? []).filter((c) => c.personId === person.id);
  const [form, setForm] = useState<ClaimInput>(EMPTY);
  const [editing, setEditing] = useState<number | undefined>(undefined);
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const close = () => {
    setEditing(undefined);
    setForm(EMPTY);
    setError('');
  };

  const save = useMutation({
    mutationFn: (input: ClaimInput) =>
      editing === undefined
        ? postJson<Claim>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/claims?user=${encodeURIComponent(user)}`, input)
        : putJson<Claim>(`/api/sheets/${encodeURIComponent(sheet)}/claims/${editing}?user=${encodeURIComponent(user)}`, input),
    onSuccess: async () => {
      close();
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
    onError: () => setError('That claim could not be saved.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!SIGNED_AMOUNT.test(form.amount.trim())) {
      setError('Type an amount with at most two decimals.');
      return;
    }
    const date = form.date.trim();
    if (date && !/^\d{4}-\d{2}-\d{2}$/.test(date)) {
      setError('Type a date as yyyy-mm-dd.');
      return;
    }
    once(() => save.mutateAsync(form));
  };

  const change = (patch: Partial<ClaimInput>) => {
    setForm((f) => ({ ...f, ...patch }));
    setError('');
  };

  const edit = (c: Claim) => {
    setEditing(c.id);
    setForm({ amount: c.amount, date: c.date, note: c.note, corrects: c.corrects === null ? '' : String(c.corrects) });
    setError('');
  };

  return (
    <td data-testid="person-claims">
      <ul>
        {claims.map((c) => (
          <li key={c.id} data-testid={`claim-row-${c.id}`}>
            <span data-testid="claim-amount">{c.amount}</span> <span data-testid="claim-date">{c.date}</span>{' '}
            <span data-testid="claim-note" style={{ whiteSpace: 'pre' }}>
              {c.note}
            </span>{' '}
            <ClaimRowValues.Slot claim={c} person={person} />{' '}
            {editing === c.id ? (
              <button type="button" data-testid="claim-cancel" onClick={close}>
                Cancel
              </button>
            ) : (
              <button type="button" data-testid="claim-edit" onClick={() => edit(c)}>
                Edit
              </button>
            )}
          </li>
        ))}
      </ul>
      <form onSubmit={submit} data-testid="claim-form">
        <input
          data-testid="claim-form-amount"
          placeholder="Amount"
          value={form.amount}
          onChange={(e) => change({ amount: e.target.value })}
        />
        <input
          data-testid="claim-form-date"
          placeholder="yyyy-mm-dd"
          value={form.date}
          onChange={(e) => change({ date: e.target.value })}
        />
        <input
          data-testid="claim-form-note"
          placeholder="Note"
          value={form.note}
          onChange={(e) => change({ note: e.target.value })}
        />
        <input
          data-testid="claim-form-corrects"
          placeholder="Corrects claim no."
          value={form.corrects}
          onChange={(e) => change({ corrects: e.target.value })}
        />
        <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
          {editing === undefined ? 'Add claim' : 'Save claim'}
        </button>
        {error && <span data-testid="claim-form-error">{error}</span>}
      </form>
    </td>
  );
}
