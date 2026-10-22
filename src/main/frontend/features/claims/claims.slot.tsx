import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonRow } from '../../slots/defs/personRow';
import { gradeOn, usePeople } from '../people/people';
import type { Person } from '../people/people';
import { useFloor, useLate } from '../settings/settings';
import { claimDue, rateOn, useRates } from '../rates/rates';
import { periodOf, usePeriods } from '../periods/periods';
import { useSheetToday } from '../sheet/sheet';
import { claimsKey, dueBy, live, overdue, sumAmounts, useClaims, useEditingClaim } from './claims';
import type { Claim } from './claims';
import { setSelected, useSelectedClaims } from './selection';

const AMOUNT = /^-?\d+(\.\d{1,2})?$/;
const NUMBER = /^(\d+|.+-\d+)$/;

/** One form on the person's row that adds a claim, or edits the claim whose editor is open. A blank
 *  date means the sheet's today. A negative amount is a correction, which names the number of the SETTLED claim
 *  of the same person it corrects (the server checks that claim). A new claim the server refuses clears the form,
 *  as one it adds does, leaving its error showing. */
function ClaimForm({ sheetId, user, person, editing, onAdded }: {
  sheetId: string; user: string; person: Person; editing?: Claim; onAdded: () => void;
}) {
  const queryClient = useQueryClient();
  const today = useSheetToday(sheetId);
  const [, setEditing] = useEditingClaim();
  const [amount, setAmount] = useState(editing?.amount ?? '');
  const [date, setDate] = useState(editing?.date ?? '');
  const [note, setNote] = useState(editing?.note ?? '');
  const [corrects, setCorrects] = useState(editing?.corrects ?? '');
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (body: { user: string; amount: string; date: string; note: string; corrects: string }) =>
      editing
        ? putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${editing.id}`, body)
        : postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/claims`, body),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
      if (editing) setEditing(undefined);
      else onAdded();
    },
    onError: (e) => {
      setError(e instanceof HttpError && (e.status === 409 || e.status === 400) && e.body
        ? e.body
        : 'Could not save this claim: the amount must be 0.00 or more with up to two decimals, and the date yyyy-mm-dd.');
      // a refused new claim is consumed as an added one is: the next starts from a blank form (date = today)
      if (!editing) {
        setAmount('');
        setDate('');
        setNote('');
        setCorrects('');
      }
    },
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!AMOUNT.test(amount.trim())) {
      setError('The amount must be a number with up to two decimals.');
      return;
    }
    const negative = amount.trim().startsWith('-') && Number(amount.trim()) < 0;
    if (negative && !NUMBER.test(corrects.trim())) {
      setError('A negative amount is a correction: name the number of the SETTLED claim it corrects.');
      return;
    }
    if (!negative && corrects.trim() !== '') {
      setError('Only a correction (a negative amount) names a claim it corrects.');
      return;
    }
    if (!once.start()) return;
    save.mutate({ user, amount: amount.trim(), date: date.trim(), note, corrects: corrects.trim() });
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
      <input data-testid="claim-form-corrects" placeholder="Corrects no." value={corrects} onChange={change(setCorrects)} />{' '}
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
  { action: 'approve', label: 'Approve' },
  { action: 'reject', label: 'Reject' },
  { action: 'void', label: 'Void' },
  { action: 'resettle', label: 'Re-settle' },
] as const;

/** One claim with its status, its due-by and whether it is overdue, who opened it and the buttons that change it
 *  (a steward approves or rejects, with a reason, a PENDING claim, and re-settles a SETTLED one at today's figure). A change the status does not
 *  allow, a settle (or approve) by the user who opened it, or an approve or reject by a clerk, is refused by the server and shown as the claim's error;
 *  so is editing a claim that is no longer a DRAFT, or one dated in a closed period (the server refuses those
 *  edits too). A steward re-dates an OPEN or PENDING claim; the server refuses anyone else, a date after its person's
 *  retirement, and a claim or new date inside a closed period. */
function ClaimRow({ sheetId, user, claim, rate }: { sheetId: string; user: string; claim: Claim; rate?: string }) {
  const queryClient = useQueryClient();
  const [, setEditing] = useEditingClaim();
  const [error, setError] = useState('');
  const [reason, setReason] = useState('');
  const period = periodOf(usePeriods(sheetId).data ?? [], claim.date);
  const floor = useFloor(sheetId);
  const today = useSheetToday(sheetId);
  const late = useLate(sheetId);
  const selected = useSelectedClaims(sheetId).has(claim.id);
  const others = (usePeople(sheetId).data ?? []).filter((p) => p.id !== claim.personId);
  const [moveTo, setMoveTo] = useState('');
  const [deferUntil, setDeferUntil] = useState('');
  const [redateTo, setRedateTo] = useState('');
  const [splitAmount, setSplitAmount] = useState('');

  const change = useMutation({
    mutationFn: (action: string) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/status`, action === 'reject' ? { user, action, reason } : { user, action }),
    onSuccess: async (_, action) => {
      if (action === 'reject') setReason('');
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e, action) =>
      setError(e instanceof HttpError && (e.status === 409 || e.status === 400) && e.body ? e.body : `Could not ${action} this ${claim.status} claim.`),
  });

  const reassign = useMutation({
    mutationFn: (personId: string) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/reassign`, { user, personId }),
    onSuccess: async () => {
      setMoveTo('');
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e) =>
      setError(e instanceof HttpError && (e.status === 409 || e.status === 400 || e.status === 404) && e.body
        ? e.body : `Could not move this ${claim.status} claim.`),
  });

  const proof = useMutation({
    mutationFn: () =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/proof`, { user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e) =>
      setError(e instanceof HttpError && (e.status === 409 || e.status === 400 || e.status === 404) && e.body
        ? e.body : `Could not add a proof to this ${claim.status} claim.`),
  });

  const defer = useMutation({
    mutationFn: (until: string) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/defer`, { user, until }),
    onSuccess: async () => {
      setDeferUntil('');
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e) =>
      setError(e instanceof HttpError && (e.status === 409 || e.status === 400 || e.status === 404) && e.body
        ? e.body : `Could not defer this ${claim.status} claim.`),
  });

  const redate = useMutation({
    mutationFn: (date: string) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/redate`, { user, date }),
    onSuccess: async () => {
      setRedateTo('');
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e) =>
      setError(e instanceof HttpError && (e.status === 409 || e.status === 400 || e.status === 404) && e.body
        ? e.body : `Could not re-date this ${claim.status} claim.`),
  });

  const split = useMutation({
    mutationFn: (amount: string) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/split`, { user, amount }),
    onSuccess: async () => {
      setSplitAmount('');
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e) =>
      setError(e instanceof HttpError && (e.status === 409 || e.status === 400 || e.status === 404) && e.body
        ? e.body : `Could not split this ${claim.status} claim.`),
  });

  return (
    <li data-testid={`claim-row-${claim.id}`}>
      <input
        type="checkbox"
        data-testid="claim-select"
        checked={selected}
        onChange={(e) => setSelected(sheetId, claim.id, e.target.checked)}
      />{' '}
      <span data-testid="claim-amount">{claim.amount}</span>{' '}
      <span data-testid="claim-date">{claim.date}</span>{' '}
      <span data-testid="claim-note">{claim.note}</span>{' '}
      <span data-testid="claim-number">{claim.number ?? ''}</span>{' '}
      Corrects <span data-testid="claim-corrects">{claim.corrects ?? ''}</span>{' '}
      <span data-testid="claim-status">{claim.status}</span>{' '}
      <span data-testid="claim-opened-by">{claim.openedBy ?? ''}</span>{' '}
      <span data-testid="claim-due">{claimDue(claim, rate, floor, late)}</span>{' '}
      Due by <span data-testid="claim-due-by">{dueBy(claim)}</span>{' '}
      Overdue <span data-testid="claim-overdue">{overdue(claim, today) ? 'yes' : 'no'}</span>{' '}
      Period <span data-testid="claim-period">{period?.name ?? ''}</span>{' '}
      Stated <span data-testid="claim-stated">{claim.stated ? 'yes' : 'no'}</span>{' '}
      Deferred <span data-testid="claim-deferred">{claim.deferred ? 'yes' : 'no'}</span>{' '}
      until <span data-testid="claim-deferred-until">{claim.deferredUntil ?? ''}</span>{' '}
      Proofs <span data-testid="claim-proofs">{claim.proofs}</span>{' '}
      Split from <span data-testid="claim-split-from">{claim.splitFrom ?? ''}</span>{' '}
      <ol data-testid="claim-log">
        {claim.log.map((line, i) => (
          <li key={i} data-testid="claim-log-line">{line}</li>
        ))}
      </ol>{' '}
      <button
        type="button"
        data-testid="claim-edit"
        onClick={() => {
          if (claim.status !== 'DRAFT') {
            setError(`A ${claim.status} claim cannot be edited; only a DRAFT can. Void it and add a new one.`);
            return;
          }
          if (period?.closed) {
            setError(`This claim is dated in the closed period ${period.name}; it cannot be edited.`);
            return;
          }
          setError('');
          setEditing(String(claim.id));
        }}
      >
        Edit
      </button>
      <input
        data-testid="claim-reject-reason"
        placeholder="Reject reason"
        value={reason}
        onChange={(e) => {
          setError('');
          setReason(e.target.value);
        }}
      />{' '}
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
      ))}{' '}
      <select
        data-testid="claim-reassign-to"
        value={moveTo}
        onChange={(e) => {
          setError('');
          setMoveTo(e.target.value);
        }}
      >
        <option value="">Move to…</option>
        {others.map((p) => (
          <option key={p.id} value={String(p.id)}>
            {p.name}
          </option>
        ))}
      </select>{' '}
      <button
        type="button"
        data-testid="claim-reassign"
        disabled={reassign.isPending}
        onClick={() => {
          setError('');
          if (moveTo === '') {
            setError('Choose the person to move this claim to.');
            return;
          }
          reassign.mutate(moveTo);
        }}
      >
        Move
      </button>{' '}
      <button
        type="button"
        data-testid="claim-proof"
        disabled={proof.isPending}
        onClick={() => {
          setError('');
          proof.mutate();
        }}
      >
        Add proof
      </button>{' '}
      <input
        data-testid="claim-defer-date"
        placeholder="Defer until yyyy-mm-dd"
        value={deferUntil}
        onChange={(e) => {
          setError('');
          setDeferUntil(e.target.value);
        }}
      />{' '}
      <button
        type="button"
        data-testid="claim-defer"
        disabled={defer.isPending}
        onClick={() => {
          setError('');
          if (!/^\d{4}-\d{2}-\d{2}$/.test(deferUntil.trim())) {
            setError('Give the date to defer this claim until as yyyy-mm-dd.');
            return;
          }
          defer.mutate(deferUntil.trim());
        }}
      >
        Defer
      </button>{' '}
      <input
        data-testid="claim-date-new"
        placeholder="Re-date to yyyy-mm-dd"
        value={redateTo}
        onChange={(e) => {
          setError('');
          setRedateTo(e.target.value);
        }}
      />{' '}
      <button
        type="button"
        data-testid="claim-redate"
        disabled={redate.isPending}
        onClick={() => {
          setError('');
          if (!/^\d{4}-\d{2}-\d{2}$/.test(redateTo.trim())) {
            setError('Give the new date of this claim as yyyy-mm-dd.');
            return;
          }
          redate.mutate(redateTo.trim());
        }}
      >
        Re-date
      </button>{' '}
      <input
        data-testid="claim-split-amount"
        placeholder="Split off amount"
        value={splitAmount}
        onChange={(e) => {
          setError('');
          setSplitAmount(e.target.value);
        }}
      />{' '}
      <button
        type="button"
        data-testid="claim-split"
        disabled={split.isPending}
        onClick={() => {
          setError('');
          if (!/^\d+(\.\d{1,2})?$/.test(splitAmount.trim())) {
            setError('The amount to split off must be a number with up to two decimals.');
            return;
          }
          split.mutate(splitAmount.trim());
        }}
      >
        Split
      </button>
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
            <ClaimRow key={c.id} sheetId={sheetId} user={user} claim={c} rate={rateOn(rates, gradeOn(person, c.date), c.date)} />
          ))}
        </ul>
      </td>
    </>
  );
}

export const contribution = PersonRow.fill({ order: 10, Component: PersonClaims });
