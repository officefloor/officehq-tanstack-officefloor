import { useIsMutating, useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { postJson, putJson } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimRow';
import { usePeople } from '../people/people.slot';
import { periodOn, usePeriods } from '../periods/periods.slot';
import { useRoles } from '../access/useAccess';
import { useUser } from '../session/useSession';
import { HttpError } from '../../api/http';
import { claimLockKey, claimSaveKey, claimsKey, useEditingClaim, type Claim, type ClaimStatus } from './useClaims';

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
// A steward can re-date an OPEN or PENDING claim (not to a date after its person's retirement, nor one in a closed
// period): its due-by and any statement figure stay, and so its period (that of its due-by) and its number stay too.
function ClaimStatusCell({ sheetId, claim }: { sheetId: string; user: string; personId: number; claim: Claim }) {
  const [user] = useUser();
  const queryClient = useQueryClient();
  const [reason, setReason] = useState('');
  const [moveTo, setMoveTo] = useState('');
  const [deferUntil, setDeferUntil] = useState('');
  const [splitAmount, setSplitAmount] = useState('');
  const [newDate, setNewDate] = useState('');
  const people = usePeople(sheetId);
  const [editingId, setEditing] = useEditingClaim();
  const periods = usePeriods(sheetId);
  const roles = useRoles(sheetId);
  // An edit the screen can already tell is refused shows its refusal at once, before any request: the claim form must
  // not be taken for its editor. The server refuses the same edits (and any it alone can tell).
  const [refused, setRefused] = useState<string>();
  // A viewer's edit is refused too, but its editor opens (to be cancelled), as for a lock another user holds.
  const viewer = () => (roles.data ?? []).some((r) => r.user === user && r.role === 'viewer');
  const refusal = (): string | undefined => {
    if (viewer()) return 'A viewer may not change anything on this sheet';
    if (claim.status !== 'DRAFT') return 'Only a DRAFT claim can be edited; void it and add a new one instead';
    if (periodOn(periods.data ?? [], claim.dueBy)?.closed) return "That claim's date is in a closed period";
    return undefined;
  };
  const base = `/api/sheets/${encodeURIComponent(sheetId)}/claims`;
  // Opening the claim's editor takes its lock. A claim that cannot be edited is refused and its editor stays shut; one
  // whose lock another user holds (409) is refused too, but its editor opens, to be saved once that user lets it go.
  // Opening another claim's editor lets go of the one open before.
  const lock = useMutation({
    mutationKey: claimLockKey(sheetId),
    mutationFn: () => postJson<Claim>(`${base}/${claim.id}/lock`, { by: user ?? '' }),
    onSuccess: () => openEditor(),
    onError: (e) => {
      if (e instanceof HttpError && e.status === 409) openEditor();
    },
  });
  // A save of the claim's open editor is its next action: the lock's refusal goes.
  const saving = useIsMutating({ mutationKey: claimSaveKey(sheetId) }) > 0 && editingId === claim.id;
  const resetLock = lock.reset;
  useEffect(() => {
    if (saving) resetLock();
  }, [saving, resetLock]);
  const openEditor = () => {
    if (editingId !== undefined && editingId !== claim.id) unlock(editingId);
    setEditing(claim.id);
  };
  const unlock = (id: number) => {
    void postJson(`${base}/${id}/unlock`, { by: user ?? '' }).catch(() => {
      // a lock not let go of is dropped when the sheet's today changes
    });
  };
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
  // A steward's re-date of an OPEN or PENDING claim: its due-by and any statement figure stay, and with them its period.
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
    lock.reset();
    setRefused(undefined);
    change.mutate(to);
  };
  const resetAll = () => {
    change.reset();
    reassign.reset();
    resettle.reset();
    defer.reset();
    proof.reset();
    split.reset();
    redate.reset();
    lock.reset();
    setRefused(undefined);
  };
  const busy = lock.isPending || change.isPending || reassign.isPending || resettle.isPending || defer.isPending || proof.isPending || split.isPending
    || redate.isPending;
  const error = (refused === undefined ? null : new Error(refused)) ?? lock.error ?? change.error ?? reassign.error ?? resettle.error ?? defer.error ?? proof.error ?? split.error ?? redate.error;

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
          lock.reset();
          setRefused(undefined);
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
          lock.reset();
          setRefused(undefined);
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
          lock.reset();
          setRefused(undefined);
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
          lock.reset();
          setRefused(undefined);
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
          lock.reset();
          setRefused(undefined);
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
          lock.reset();
          setRefused(undefined);
          redate.mutate(newDate);
        }}
      >
        Re-date
      </button>{' '}
      <button
        type="button"
        data-testid="claim-edit"
        disabled={busy}
        onClick={() => {
          resetAll();
          const why = refusal();
          if (why === undefined) lock.mutate();
          else {
            setRefused(why);
            if (viewer()) openEditor();
          }
        }}
      >
        Edit
      </button>
      {editingId === claim.id && (
        <button
          type="button"
          data-testid="claim-cancel"
          onClick={() => {
            resetAll();
            unlock(claim.id);
            setEditing(undefined);
          }}
        >
          Cancel
        </button>
      )}{' '}
      {error && (
        <span data-testid="claim-error" role="alert">
          {error.message}
        </span>
      )}
    </>
  );
}

export const contribution = ClaimCells.fill({ order: 20, Component: ClaimStatusCell });
