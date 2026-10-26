import { useIsMutating, useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { postJson, putJson } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimRow';
import { usePeople } from '../people/people.slot';
import { periodOn, usePeriods } from '../periods/periods.slot';
import { useDelegations, useRoles } from '../access/useAccess';
import { useSheetToday } from '../today/today.slot';
import { useUser } from '../session/useSession';
import { useVoidReasons } from '../voidreasons/voidReasons.slot';
import { HttpError } from '../../api/http';
import { claimLockKey, claimSaveKey, claimsKey, useEditingClaim, type Claim, type ClaimStatus } from './useClaims';

// `review`: a steward's approve (to SETTLED) or reject (back to OPEN, with a reason) of a PENDING claim.
const AMOUNT = /^-?\d+(\.\d{1,2})?$/;
const CLOSED = "That claim's date is in a closed period";

// `release`: a steward's review of a REVIEW claim, opening it (it takes its number then).
type Change = { status: ClaimStatus; review?: boolean; release?: boolean; reason?: string; voidReason?: string };

const ACTIONS: Array<{ action: string; change: Change; label: string }> = [
  { action: 'open', change: { status: 'OPEN' }, label: 'Open' },
  { action: 'settle', change: { status: 'SETTLED' }, label: 'Settle' },
  { action: 'void', change: { status: 'VOID' }, label: 'Void' },
  { action: 'approve', change: { status: 'SETTLED', review: true }, label: 'Approve' },
  { action: 'review', change: { status: 'OPEN', release: true }, label: 'Review' },
  { action: 'delete', change: { status: 'DELETED' }, label: 'Delete' },
  { action: 'restore', change: { status: 'DRAFT' }, label: 'Restore' },
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
// An OPEN or PENDING claim can be reminded once per sheet-today: it shows the last day it was reminded.
// A steward can waive an OPEN or PENDING claim's late charge, once: waived, it bears none from then on.
// A claim in any status can be commented; an empty comment is refused.
// A DRAFT claim (not in a closed period) can be deleted: DELETED, it counts nowhere and takes no edit, open or void; on
// the sheet-today it was deleted it can be restored to a DRAFT, and on a later day the restore is refused.
// A DRAFT claim (not in a closed period, nor held open by another user's editor) takes lines — a label, a whole quantity
// of 1 or more and a unit amount — and its amount becomes their total; adding one counts as editing the claim.
// A DRAFT's amount can be changed in the row, beside its edit button, under the claim form's rules (refused as its edit is).
function ClaimStatusCell({ sheetId, claim }: { sheetId: string; user: string; personId: number; claim: Claim }) {
  const [user] = useUser();
  const queryClient = useQueryClient();
  const [reason, setReason] = useState('');
  const [voidReason, setVoidReason] = useState('');
  const voidReasons = useVoidReasons(sheetId);
  const [moveTo, setMoveTo] = useState('');
  const [deferUntil, setDeferUntil] = useState('');
  const [splitAmount, setSplitAmount] = useState('');
  const [newDate, setNewDate] = useState('');
  const [inlineAmount, setInlineAmount] = useState('');
  const [commentText, setCommentText] = useState('');
  const [lineLabel, setLineLabel] = useState('');
  const [lineQty, setLineQty] = useState('');
  const [lineUnit, setLineUnit] = useState('');
  const people = usePeople(sheetId);
  const [editingId, setEditing] = useEditingClaim();
  const periods = usePeriods(sheetId);
  const roles = useRoles(sheetId);
  const delegations = useDelegations(sheetId);
  const sheetToday = useSheetToday(sheetId);
  // An edit the screen can already tell is refused shows its refusal at once, before any request: the claim form must
  // not be taken for its editor. The server refuses the same edits (and any it alone can tell).
  const [refused, setRefused] = useState<string>();
  // A viewer's edit, and one of a claim in a closed period, is refused too, but its editor opens (to be cancelled), as
  // for a lock another user holds; its save is refused in turn.
  // One a steward's delegation holds on the sheet's today acts as a steward, as the server decides.
  const viewer = () => {
    const assigned = roles.data ?? [];
    const steward = (name: string) => assigned.some((r) => r.user === name && r.role === 'steward');
    const today = sheetToday.data?.today;
    const acting = today !== undefined && (delegations.data ?? []).some((d) =>
      d.user === user && steward(d.by) && d.from <= today && today <= d.to);
    return !acting && assigned.some((r) => r.user === user && r.role === 'viewer');
  };
  const refusal = (): string | undefined => {
    if (viewer()) return 'A viewer may not change anything on this sheet';
    if (claim.status !== 'DRAFT') return 'Only a DRAFT claim can be edited; void it and add a new one instead';
    if (periodOn(periods.data ?? [], claim.dueBy)?.closed) return CLOSED;
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
      setVoidReason('');
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
  // An OPEN or PENDING claim is reminded once per sheet-today; it then shows that day as its reminded date.
  const remind = useMutation({
    mutationFn: () =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/remind`, { by: user ?? '' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });
  // A steward's waiver of an OPEN or PENDING claim's late charge: it bears none from then on.
  const waive = useMutation({
    mutationFn: () =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${claim.id}/waive`, { by: user ?? '' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });
  // A comment kept on the claim with the user and the sheet's today; the server refuses an empty one too.
  const comment = useMutation({
    mutationFn: (text: string) => {
      if (text.trim() === '') throw new Error('Enter a comment');
      return postJson<Claim>(`${base}/${claim.id}/comment`, { text: text.trim(), by: user ?? '' });
    },
    onSuccess: () => {
      setCommentText('');
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });
  // A DRAFT's amount changed in the row: the claim form's edit, under its rules, keeping the claim's date, note, corrects, reference and tags.
  const inline = useMutation({
    mutationFn: (amount: string) => {
      const why = refusal();
      if (why !== undefined) throw new Error(why);
      if (amount.trim() === '') throw new Error('Enter an amount');
      if (!AMOUNT.test(amount.trim())) throw new Error('An amount is a number with up to two decimals');
      return putJson<Claim>(`${base}/${claim.id}`, { amount: amount.trim(), date: claim.date, note: claim.note,
        corrects: claim.corrects ?? '', ref: claim.ref ?? '', tags: (claim.tags ?? []).join(','), by: user ?? '' });
    },
    onSuccess: () => {
      setInlineAmount('');
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });
  // A line added to a DRAFT; the server refuses one on a claim locked by another user, and checks the fields too.
  const line = useMutation({
    mutationFn: (f: { label: string; qty: string; unit: string }) => {
      const why = refusal();
      if (why !== undefined) throw new Error(why);
      if (f.label.trim() === '') throw new Error("Enter the line's label");
      if (!/^\d+$/.test(f.qty.trim()) || Number(f.qty.trim()) < 1) throw new Error("A line's quantity is a whole number, at least 1");
      if (!/^\d+(\.\d{1,2})?$/.test(f.unit.trim())) throw new Error('A unit amount is a number, 0.00 or more, with up to two decimals');
      return postJson<Claim>(`${base}/${claim.id}/line`,
        { label: f.label.trim(), qty: f.qty.trim(), unit: f.unit.trim(), by: user ?? '' });
    },
    onSuccess: () => {
      setLineLabel('');
      setLineQty('');
      setLineUnit('');
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
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
    remind.reset();
    waive.reset();
    inline.reset();
    comment.reset();
    line.reset();
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
    remind.reset();
    waive.reset();
    inline.reset();
    comment.reset();
    line.reset();
    setRefused(undefined);
  };
  const busy = lock.isPending || change.isPending || reassign.isPending || resettle.isPending || defer.isPending || proof.isPending || split.isPending
    || redate.isPending || remind.isPending || waive.isPending || inline.isPending || comment.isPending || line.isPending;
  const error = (refused === undefined ? null : new Error(refused)) ?? lock.error ?? change.error ?? reassign.error ?? resettle.error ?? defer.error ?? proof.error ?? split.error ?? redate.error ?? remind.error ?? waive.error ?? inline.error ?? comment.error ?? line.error;

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
          onClick={() => changeTo(a.action === 'void' ? { ...a.change, voidReason } : a.change)}
        >
          {a.label}
        </button>
      ))}{' '}
      <select
        data-testid="claim-void-reason"
        value={voidReason}
        onChange={(e) => {
          setVoidReason(e.target.value);
          change.reset();
        }}
      >
        <option value="">Void reason…</option>
        {(voidReasons.data ?? []).map((r) => (
          <option key={r.reason} value={r.reason}>
            {r.reason}
          </option>
        ))}
      </select>{' '}
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
          remind.reset();
    waive.reset();
          waive.reset();
          inline.reset();
          comment.reset();
          line.reset();
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
          remind.reset();
    waive.reset();
          waive.reset();
          inline.reset();
          comment.reset();
          line.reset();
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
          remind.reset();
    waive.reset();
          waive.reset();
          inline.reset();
          comment.reset();
          line.reset();
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
          remind.reset();
    waive.reset();
          waive.reset();
          inline.reset();
          comment.reset();
          line.reset();
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
          remind.reset();
    waive.reset();
          waive.reset();
          inline.reset();
          comment.reset();
          line.reset();
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
          remind.reset();
    waive.reset();
          waive.reset();
          inline.reset();
          comment.reset();
          line.reset();
          setRefused(undefined);
          redate.mutate(newDate);
        }}
      >
        Re-date
      </button>{' '}
      Reminded <span data-testid="claim-reminded">{claim.reminded ?? ''}</span>{' '}
      <button
        type="button"
        data-testid="claim-remind"
        disabled={busy}
        onClick={() => {
          resetAll();
          remind.mutate();
        }}
      >
        Remind
      </button>{' '}
      Waived <span data-testid="claim-waived">{claim.waived ? 'yes' : 'no'}</span>{' '}
      <button
        type="button"
        data-testid="claim-waive"
        disabled={busy}
        onClick={() => {
          resetAll();
          waive.mutate();
        }}
      >
        Waive
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
            if (viewer() || why === CLOSED) openEditor();
          }
        }}
      >
        Edit
      </button>{' '}
      <input
        data-testid="claim-inline-amount"
        placeholder="Amount"
        inputMode="decimal"
        value={inlineAmount}
        onChange={(e) => {
          setInlineAmount(e.target.value);
          inline.reset();
          comment.reset();
        }}
      />
      <button
        type="button"
        data-testid="claim-inline-save"
        disabled={busy}
        onClick={() => {
          resetAll();
          inline.mutate(inlineAmount);
        }}
      >
        Save amount
      </button>{' '}
      <input
        data-testid="claim-comment-text"
        placeholder="Comment"
        value={commentText}
        onChange={(e) => {
          setCommentText(e.target.value);
          comment.reset();
        }}
      />
      <button
        type="button"
        data-testid="claim-comment-add"
        disabled={busy}
        onClick={() => {
          resetAll();
          comment.mutate(commentText);
        }}
      >
        Comment
      </button>{' '}
      <input
        data-testid="claim-line-label"
        placeholder="Line"
        value={lineLabel}
        onChange={(e) => {
          setLineLabel(e.target.value);
          line.reset();
        }}
      />
      <input
        data-testid="claim-line-qty"
        placeholder="Qty"
        inputMode="numeric"
        value={lineQty}
        onChange={(e) => {
          setLineQty(e.target.value);
          line.reset();
        }}
      />
      <input
        data-testid="claim-line-unit"
        placeholder="Unit"
        inputMode="decimal"
        value={lineUnit}
        onChange={(e) => {
          setLineUnit(e.target.value);
          line.reset();
        }}
      />
      <button
        type="button"
        data-testid="claim-line-add"
        disabled={busy}
        onClick={() => {
          resetAll();
          line.mutate({ label: lineLabel, qty: lineQty, unit: lineUnit });
        }}
      >
        Add line
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
