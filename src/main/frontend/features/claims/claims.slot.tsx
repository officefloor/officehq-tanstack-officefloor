import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonRow } from '../../slots/defs/personRow';
import type { Person } from '../people/people';
import { useFloor } from '../settings/settings';
import { claimDue, rateOn, useRates } from '../rates/rates';
import { periodOf, usePeriods } from '../periods/periods';
import { useSheetToday } from '../sheet/sheet';
import { claimsKey, live, sumAmounts, useClaims, useEditingClaim } from './claims';
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
  const once = useSubmitOnce();

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
    onError: (e) =>
      setError(e instanceof HttpError && e.status === 409 && e.body
        ? e.body
        : 'Could not save this claim: the amount must be 0.00 or more with up to two decimals, and the date yyyy-mm-dd.'),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!AMOUNT.test(amount.trim())) {
      setError('The amount must be a number, 0.00 or more, with up to two decimals.');
      return;
    }
    if (!once.start()) return;
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

const ACTIONS = [
  { action: 'open', label: 'Open' },
  { action: 'settle', label: 'Settle' },
  { action: 'void', label: 'Void' },
] as const;

/** One claim with its status, who opened it and the buttons that change it. A change the status does not
 *  allow, or a settle by the user who opened it, is refused by the server and shown as the claim's error;
 *  so is editing a claim that is no longer a DRAFT (the server refuses that edit too). */
function ClaimRow({ sheetId, user, claim, rate }: { sheetId: string; user: string; claim: Claim; rate?: string }) {
  const queryClient = useQueryClient();
  const [, setEditing] = useEditingClaim();
  const [error, setError] = useState('');
  const period = periodOf(usePeriods(sheetId).data ?? [], claim.date);
  const floor = useFloor(sheetId);

  const change = useMutation({
    mutationFn: (action: string) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/status`, { user, action }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e, action) =>
      setError(e instanceof HttpError && e.status === 409 && e.body ? e.body : `Could not ${action} this ${claim.status} claim.`),
  });

  return (
    <li data-testid={`claim-row-${claim.id}`}>
      <span data-testid="claim-amount">{claim.amount}</span>{' '}
      <span data-testid="claim-date">{claim.date}</span>{' '}
      <span data-testid="claim-note">{claim.note}</span>{' '}
      <span data-testid="claim-number">{claim.number ?? ''}</span>{' '}
      <span data-testid="claim-status">{claim.status}</span>{' '}
      <span data-testid="claim-opened-by">{claim.openedBy ?? ''}</span>{' '}
      <span data-testid="claim-due">{claimDue(claim, rate, floor)}</span>{' '}
      Period <span data-testid="claim-period">{period?.name ?? ''}</span>{' '}
      Stated <span data-testid="claim-stated">{claim.stated ? 'yes' : 'no'}</span>{' '}
      <button
        type="button"
        data-testid="claim-edit"
        onClick={() => {
          if (claim.status !== 'DRAFT') {
            setError(`A ${claim.status} claim cannot be edited; only a DRAFT can. Void it and add a new one.`);
            return;
          }
          setError('');
          setEditing(String(claim.id));
        }}
      >
        Edit
      </button>
      {ACTIONS.map(({ action, label }) => (
        <button
          key={action}
          type="button"
          data-testid={`claim-${action}`}
          disabled={change.isPending}
          onClick={() => {
            setError('');
            change.mutate(action);
          }}
        >
          {label}
        </button>
      ))}
      {error && (
        <p data-testid="claim-error" role="alert">
          {error}
        </p>
      )}
    </li>
  );
}

/** A person's claimed total and their claims, newest first, with the form that adds or edits them. */
function PersonClaims({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const claims = (useClaims(sheetId).data ?? []).filter((c) => c.personId === person.id);
  const [editingId] = useEditingClaim();
  const editing = claims.find((c) => String(c.id) === editingId);
  const [added, setAdded] = useState(0);
  const rates = useRates(sheetId).data ?? [];

  return (
    <>
      <td data-testid="person-claimed">{sumAmounts(live(claims, [person]))}</td>
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
            <ClaimRow key={c.id} sheetId={sheetId} user={user} claim={c} rate={rateOn(rates, person.grade, c.date)} />
          ))}
        </ul>
      </td>
    </>
  );
}

export const contribution = PersonRow.fill({ order: 10, Component: PersonClaims });
