import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { postJson, putJson } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimRow';
import { usePeople } from '../people/people.slot';
import { useUser } from '../session/useSession';
import { claimsKey, type Claim, type ClaimStatus } from './useClaims';

// `review`: a steward's approve (to SETTLED) or reject (back to OPEN, with a reason) of a PENDING claim.
type Change = { status: ClaimStatus; review?: boolean; reason?: string };

const ACTIONS: Array<{ action: string; change: Change; label: string }> = [
  { action: 'open', change: { status: 'OPEN' }, label: 'Open' },
  { action: 'settle', change: { status: 'SETTLED' }, label: 'Settle' },
  { action: 'void', change: { status: 'VOID' }, label: 'Void' },
  { action: 'approve', change: { status: 'SETTLED', review: true }, label: 'Approve' },
];

// A claim's number and status, the buttons that open, settle, void, approve or reject it, and a steward's move of a DRAFT,
// OPEN or PENDING claim to another person (listed then as that person's newest claim). A change its status does not
// allow, a settle or approve by the user who opened the claim, or an approve or reject by a non-steward, is refused by the
// server, and the refusal shows on the claim until its next action (only the last action's error shows). A clerk's settle above the clerk limit leaves it PENDING.
// A steward may void a SETTLED claim within 7 days of its settlement (not in a closed period): its frozen due still shows but
// counts in no total.
// An OPEN claim can be deferred to a date after the sheet's today (not in a closed period): until then it counts in no
// total, is not overdue and cannot be settled; whether it is deferred, and until when, shows on the claim.
// A claim not SETTLED or VOID takes a proof, raising its proofs count by one; one whose due is above the proof threshold
// in force on its date cannot be settled or approved while it has none (refused before the clerk limit is considered).
// An OPEN claim (not deferred, not in a closed period) can be split: an amount above 0.00 and below its own goes to a new
// OPEN claim with the next number, listed as the person's newest and showing the claim's number as split-from.
// A steward can re-date an OPEN or PENDING claim (not to a date after its person's retirement, nor in or into a closed
// period): its due-by and any statement figure stay, its period follows the new date, and when numbering by period it
// takes the next number of its new period when the period changes.
function ClaimStatusCell({ sheetId, claim }: { sheetId: string; user: string; personId: number; claim: Claim }) {
  const [user] = useUser();
  const queryClient = useQueryClient();
  const [reason, setReason] = useState('');
  const [moveTo, setMoveTo] = useState('');
  const [deferUntil, setDeferUntil] = useState('');
  const [splitAmount, setSplitAmount] = useState('');
  const [newDate, setNewDate] = useState('');
  const people = usePeople(sheetId);
  const change = useMutation({
    mutationFn: (to: Change) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/status`, { ...to, by: user ?? '' }),
    onSuccess: () => {
      setReason('');
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });
  const reassign = useMutation({
    mutationFn: (personId: string) => {
      if (personId === '') throw new Error('Choose the person to move the claim to');
      return putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/person`,
        { personId: Number(personId), by: user ?? '' });
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });
  // A steward's re-settle of a SETTLED claim at the figure it would have today as an open claim bound by no statement.
  const resettle = useMutation({
    mutationFn: () =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/resettle`, { by: user ?? '' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });
  const defer = useMutation({
    mutationFn: (until: string) => {
      if (until.trim() === '') throw new Error('Enter the date to defer the claim until');
      return postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/defer`,
        { until: until.trim(), by: user ?? '' });
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });
  const proof = useMutation({
    mutationFn: () =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/proof`, { by: user ?? '' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });
  const split = useMutation({
    mutationFn: (amount: string) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/split`,
        { amount: amount.trim(), by: user ?? '' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });
  // A steward's re-date of an OPEN or PENDING claim: its due-by and any statement figure stay; its period follows.
  const redate = useMutation({
    mutationFn: (date: string) => {
      if (date.trim() === '') throw new Error("Enter the claim's new date");
      return postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/redate`,
        { date: date.trim(), by: user ?? '' });
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });
  // Each action on the claim clears the other's refusal, so one error shows: the last action's.
  const changeTo = (to: Change) => {
    reassign.reset();
    resettle.reset();
    defer.reset();
    proof.reset();
    split.reset();
    redate.reset();
    change.mutate(to);
  };
  const busy = change.isPending || reassign.isPending || resettle.isPending || defer.isPending || proof.isPending || split.isPending
    || redate.isPending;
  const error = change.error ?? reassign.error ?? resettle.error ?? defer.error ?? proof.error ?? split.error ?? redate.error;

  return (
    <>
      <span data-testid="claim-number">{claim.number ?? ''}</span>{' '}
      <span data-testid="claim-status">{claim.status}</span>{' '}
      <span data-testid="claim-opened-by">{claim.openedBy ?? ''}</span>{' '}
      {ACTIONS.map((a) => (
        <button
          key={a.action}
          type="button"
          data-testid={`claim-${a.action}`}
          disabled={busy}
          onClick={() => changeTo(a.change)}
        >
          {a.label}
        </button>
      ))}{' '}
      <input
        data-testid="claim-reject-reason"
        placeholder="Reject reason"
        value={reason}
        onChange={(e) => setReason(e.target.value)}
      />
      <button
        type="button"
        data-testid="claim-reject"
        disabled={busy}
        onClick={() => changeTo({ status: 'OPEN', review: true, reason })}
      >
        Reject
      </button>{' '}
      <select data-testid="claim-reassign-to" value={moveTo} onChange={(e) => setMoveTo(e.target.value)}>
        <option value="">Move to…</option>
        {(people.data ?? []).filter((p) => p.id !== claim.personId).map((p) => (
          <option key={p.id} value={String(p.id)}>
            {p.name}
          </option>
        ))}
      </select>
      <button
        type="button"
        data-testid="claim-reassign"
        disabled={busy}
        onClick={() => {
          change.reset();
          resettle.reset();
          defer.reset();
          proof.reset();
          split.reset();
          redate.reset();
          reassign.mutate(moveTo);
        }}
      >
        Move
      </button>{' '}
      <button
        type="button"
        data-testid="claim-resettle"
        disabled={busy}
        onClick={() => {
          change.reset();
          reassign.reset();
          defer.reset();
          proof.reset();
          split.reset();
          redate.reset();
          resettle.mutate();
        }}
      >
        Re-settle
      </button>{' '}
      Deferred <span data-testid="claim-deferred">{claim.deferred ? 'yes' : 'no'}</span>{' '}
      <span data-testid="claim-deferred-until">{claim.deferredUntil ?? ''}</span>{' '}
      <input
        type="date"
        data-testid="claim-defer-date"
        value={deferUntil}
        onChange={(e) => {
          setDeferUntil(e.target.value);
          defer.reset();
        }}
      />
      <button
        type="button"
        data-testid="claim-defer"
        disabled={busy}
        onClick={() => {
          change.reset();
          reassign.reset();
          resettle.reset();
          proof.reset();
          split.reset();
          redate.reset();
          defer.mutate(deferUntil);
        }}
      >
        Defer
      </button>{' '}
      Proofs <span data-testid="claim-proofs">{claim.proofs ?? 0}</span>{' '}
      <button
        type="button"
        data-testid="claim-proof"
        disabled={busy}
        onClick={() => {
          change.reset();
          reassign.reset();
          resettle.reset();
          defer.reset();
          split.reset();
          redate.reset();
          proof.mutate();
        }}
      >
        Add proof
      </button>{' '}
      Split from <span data-testid="claim-split-from">{claim.splitFrom ?? ''}</span>{' '}
      <input
        data-testid="claim-split-amount"
        placeholder="Split amount"
        value={splitAmount}
        onChange={(e) => {
          setSplitAmount(e.target.value);
          split.reset();
        }}
      />
      <button
        type="button"
        data-testid="claim-split"
        disabled={busy}
        onClick={() => {
          change.reset();
          reassign.reset();
          resettle.reset();
          defer.reset();
          proof.reset();
          redate.reset();
          split.mutate(splitAmount);
        }}
      >
        Split
      </button>{' '}
      <input
        type="date"
        data-testid="claim-date-new"
        value={newDate}
        onChange={(e) => {
          setNewDate(e.target.value);
          redate.reset();
        }}
      />
      <button
        type="button"
        data-testid="claim-redate"
        disabled={busy}
        onClick={() => {
          change.reset();
          reassign.reset();
          resettle.reset();
          defer.reset();
          proof.reset();
          split.reset();
          redate.mutate(newDate);
        }}
      >
        Re-date
      </button>{' '}
      {error && (
        <span data-testid="claim-error" role="alert">
          {error.message}
        </span>
      )}
    </>
  );
}

export const contribution = ClaimCells.fill({ order: 20, Component: ClaimStatusCell });
