import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { PersonRow } from '../../slots/defs/personRow';
import type { Person } from '../people/people';
import { dueOn, useRateFor } from '../rates/rates';
import { useSheetToday } from '../sheet/sheet';
import { claimsKey, sumAmounts, useClaims, useEditingClaim } from './claims';
import type { Claim } from './claims';

const AMOUNT = /^\d+(\.\d{1,2})?$/;

/** One form on the person's row that adds a claim, or edits the claim whose editor is open. A blank
 *  date means the sheet's today. */
function ClaimForm({ sheetId, user, person, editing, onAdded }: {
  sheetId: string; user: string; person: Person; editing?: Claim; onAdded: () => void;
}) {
  const queryClient = useQueryClient();
  const today = useSheetToday(sheetId);
  const [, setEditing] = useEditingClaim();
  const [amount, setAmount] = useState(editing?.amount ?? '');
  const [date, setDate] = useState(editing?.date ?? '');
  const [note, setNote] = useState(editing?.note ?? '');
  const [error, setError] = useState('');

  const save = useMutation({
    mutationFn: (body: { user: string; amount: string; date: string; note: string }) =>
      editing
        ? putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${editing.id}`, body)
        : postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/claims`, body),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
      if (editing) setEditing(undefined);
      else onAdded();
    },
    onError: () => setError('Could not save this claim: the amount must be 0.00 or more with up to two decimals, and the date yyyy-mm-dd.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!AMOUNT.test(amount.trim())) {
      setError('The amount must be a number, 0.00 or more, with up to two decimals.');
      return;
    }
    save.mutate({ user, amount: amount.trim(), date: date.trim(), note });
  };

  const change = (set: (v: string) => void) => (e: { target: { value: string } }) => {
    set(e.target.value);
    setError('');
  };

  return (
    <form data-testid="claim-form" onSubmit={submit}>
      <input data-testid="claim-form-amount" placeholder="Amount" value={amount} onChange={change(setAmount)} />{' '}
      <input data-testid="claim-form-date" placeholder={today || 'yyyy-mm-dd'} value={date} onChange={change(setDate)} />{' '}
      <input data-testid="claim-form-note" placeholder="Note" value={note} onChange={change(setNote)} />{' '}
      <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
        {editing ? 'Save claim' : 'Add claim'}
      </button>
      {editing && (
        <button type="button" data-testid="claim-form-cancel" onClick={() => setEditing(undefined)}>
          Cancel
        </button>
      )}
      {error && (
        <p data-testid="claim-form-error" role="alert">
          {error}
        </p>
      )}
    </form>
  );
}

/** A person's claimed total and their claims, newest first, with the form that adds or edits them. */
function PersonClaims({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const claims = (useClaims(sheetId).data ?? []).filter((c) => c.personId === person.id);
  const [editingId, setEditing] = useEditingClaim();
  const editing = claims.find((c) => String(c.id) === editingId);
  const [added, setAdded] = useState(0);
  const rate = useRateFor(sheetId, person.grade);

  return (
    <>
      <td data-testid="person-claimed">{sumAmounts(claims)}</td>
      <td>
        {/* Keyed by the claim being edited (and each add), so the form fills with its values or starts blank. */}
        <ClaimForm
          key={editing ? `edit-${editing.id}` : `new-${added}`}
          sheetId={sheetId}
          user={user}
          person={person}
          editing={editing}
          onAdded={() => setAdded((n) => n + 1)}
        />
        <ul>
          {claims.map((c) => (
            <li key={c.id} data-testid={`claim-row-${c.id}`}>
              <span data-testid="claim-amount">{c.amount}</span>{' '}
              <span data-testid="claim-date">{c.date}</span>{' '}
              <span data-testid="claim-note">{c.note}</span>{' '}
              <span data-testid="claim-due">{dueOn(c.amount, rate)}</span>{' '}
              <button type="button" data-testid="claim-edit" onClick={() => setEditing(String(c.id))}>
                Edit
              </button>
            </li>
          ))}
        </ul>
      </td>
    </>
  );
}

export const contribution = PersonRow.fill({ order: 10, Component: PersonClaims });
