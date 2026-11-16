import { useEffect, useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPage } from '../../slots/defs/sheetPage';
import { gradeOn, usePeople, type Person } from '../people/people';
import { periodOf, usePeriods } from '../periods/periods';
import { useRoles } from '../roles/roles';
import { useDelegations } from '../delegation/delegation';
import { claimDue, rateFor, useRates } from '../rates/rates';
import { dueBy, isOverdue } from '../overdue/overdue';
import { useFloor, useLate, type Dated } from '../settings/settings';
import { claimsKey, commentLine, lineLine, live, logLine, claimed, useClaims, type Claim } from './claims';
import { useSelectedClaims } from './selection';
import { useSheetToday } from '../session/session';
import { useVoidReasons } from '../voidreasons/voidreasons';
import { useTemplates } from '../templates/templates';

type Values = { amount: string; date: string; note: string; corrects: string; ref: string; tags: string };
const EMPTY: Values = { amount: '', date: '', note: '', corrects: '', ref: '', tags: '' };

function ClaimsHead() {
  return (
    <>
      <th>Claimed</th>
      <th>Claims</th>
    </>
  );
}

const DRAFT_ONLY = 'Only a draft claim whose due-by is not inside a closed period can be edited; void it and add a new one instead.';
const VIEWER_REFUSED = 'A viewer cannot change the sheet.';
const LOCK_REFUSED = 'Someone else is editing this claim; it can be edited once they save or cancel.';
const EMPTY_COMMENT = 'A comment cannot be empty.';
const EDIT_REFUSED = 'This claim cannot be edited: only a draft whose due-by is not inside a closed period, and not while someone else is editing it.';

type Action = 'open' | 'settle' | 'approve' | 'reject' | 'void' | 'resettle' | 'delete' | 'restore' | 'review';

/** One claim: its select checkbox (the selection a bulk action takes), its values, status, due, its status log (oldest first), due-by (fixed when it was added), whether it is overdue, whether it is deferred and until when, its proofs count, and the period its due-by falls in, with the buttons that edit it or change its status. Open above the review threshold on its date makes it REVIEW, which only a steward's Review makes OPEN (refused, with the claim's error, for anyone else). A clerk's settle above the clerk limit makes it PENDING; a
 *  steward approves it, or rejects it with the reason typed beside Reject. An action the status does not allow,
 *  settling or approving a claim the user opened, or a clerk approving or rejecting, is refused by the server and
 *  shows the claim's error. An OPEN claim is deferred until the date typed beside Defer (after today; refused, with the claim's error, for any other claim or date). Add proof raises its proofs count by one (refused, with the claim's error, for a SETTLED or VOID claim or one whose due-by is inside a closed period); a claim with no proofs whose due is above the proof threshold on its date cannot be settled nor approved; a claim with an empty note whose due is above the note threshold on its date cannot be opened (alone or in bulk). While the sheet has void reasons, Void needs one chosen beside it (refused, with the claim's error, without one). A steward voids a SETTLED claim within 7 days of its settlement (its frozen figure leaves every total). A steward re-settles a SETTLED claim (its due fixed again at today's figure). Delete makes a DRAFT DELETED (it counts nowhere); Restore makes it a DRAFT again, only on the sheet-today it was deleted (refused, with the claim's error, otherwise). A steward re-dates an OPEN or PENDING claim to the date typed beside Re-date (its due-by and any statement figure stay; refused, with the claim's error, for anyone else, any other claim, or a date after the person's retirement). A steward moves a DRAFT, OPEN or PENDING claim to the person picked beside Move (refused,
 *  with the claim's error, for anyone else or any other claim). Remind writes a reminder for an OPEN or PENDING claim, once per sheet-today, which becomes its reminded date (refused, with the claim's error, for any other claim or a second reminder that day). Comment adds the text typed beside it to the claim's comments, shown in the order written as `<user> on <date>: <text>` (an empty one is refused with the claim's error). Split takes the amount typed beside it off an OPEN claim (not deferred, nor with its due-by inside a closed period; more than 0.00 and less than its amount; only by a role the sheet lets open a claim) into a new OPEN claim of the person, refused with the claim's error otherwise. Only a DRAFT whose due-by is not inside a closed period can be edited: Edit opens the editor and locks the claim for the user until it
 *  is saved or cancelled (Cancel) or the sheet's today changes; Edit on any other claim, on a claim another user holds, or by a
 *  viewer shows the claim's error instead and takes no lock. The row's own amount entry and Save amount change a DRAFT's amount
 *  exactly as the editor's save would (same rules, CLAIM_EDITED), keeping its date and note; a refusal shows the claim's error. Add line adds the label, quantity and unit amount typed
 *  beside it to a DRAFT's lines, shown as `<label> <qty> x <unit>`; its amount becomes their total and another amount
 *  typed in an edit is refused (the same rules as an edit, else the claim's error). */
function ClaimRow({
  sheetId,
  user,
  claim: c,
  rate,
  floor,
  editing,
  onEdit,
  onCancel,
}: {
  sheetId: string;
  user: string;
  claim: Claim;
  rate: string | undefined;
  floor: Dated;
  editing: boolean;
  onEdit: () => void;
  onCancel: () => void;
}) {
  const roles = useRoles(sheetId);
  const delegations = useDelegations(sheetId);
  const today = useSheetToday(sheetId);
  // a viewer acting for a steward who delegated to them (the sheet's today inside its range) is not a viewer then
  const viewer =
    roles.data?.find((r) => r.user === user)?.role === 'viewer' &&
    !delegations.data?.some((d) => d.user === user && d.from <= today && today <= d.to);
  const queryClient = useQueryClient();
  const periods = usePeriods(sheetId);
  const late = useLate(sheetId);
  const status = useMutation({
    mutationFn: (action: Action) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${c.id}/status`, {
        action,
        by: user,
        reason: action === 'reject' ? reason : action === 'void' ? voidReason : '',
      }),
    onSuccess: (_, action) => {
      if (action === 'reject') setReason('');
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });
  const people = usePeople(sheetId);
  const [moveTo, setMoveTo] = useState('');
  const reassign = useMutation({
    mutationFn: (personId: number) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${c.id}/person`, { personId, by: user }),
    onSuccess: () => {
      setMoveTo('');
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });
  const [reason, setReason] = useState('');
  const voidReasons = useVoidReasons(sheetId);
  const [voidReason, setVoidReason] = useState('');
  const [deferUntil, setDeferUntil] = useState('');
  const defer = useMutation({
    mutationFn: (until: string) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${c.id}/defer`, { until, by: user }),
    onSuccess: () => {
      setDeferUntil('');
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });
  const [newDate, setNewDate] = useState('');
  const redate = useMutation({
    mutationFn: (date: string) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${c.id}/date`, { date, by: user }),
    onSuccess: () => {
      setNewDate('');
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });
  const proof = useMutation({
    mutationFn: () => postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${c.id}/proofs`, { by: user }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });
  const [splitAmount, setSplitAmount] = useState('');
  const split = useMutation({
    mutationFn: (amount: string) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${c.id}/split`, { amount, by: user }),
    onSuccess: () => {
      setSplitAmount('');
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });
  const remind = useMutation({
    mutationFn: () => postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${c.id}/remind`, { by: user }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });
  const [commentText, setCommentText] = useState('');
  const comment = useMutation({
    mutationFn: (text: string) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${c.id}/comments`, { text, by: user }),
    onSuccess: () => {
      setCommentText('');
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });
  const [lineLabel, setLineLabel] = useState('');
  const [lineQty, setLineQty] = useState('');
  const [lineUnit, setLineUnit] = useState('');
  const line = useMutation({
    mutationFn: (l: { label: string; qty: string; unit: string }) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${c.id}/lines`, { ...l, by: user }),
    onSuccess: () => {
      setLineLabel('');
      setLineQty('');
      setLineUnit('');
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });
  const [inlineAmount, setInlineAmount] = useState('');
  // the row's own amount entry saves through the claim form's edit, keeping the claim's date, note, corrects, reference and tags
  const inline = useMutation({
    mutationFn: (amount: string) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${c.id}`, {
        amount,
        date: c.date,
        note: c.note,
        corrects: c.corrects ?? '',
        ref: c.ref ?? '',
        tags: c.tags.join(','),
        by: user,
      }),
    onSuccess: () => {
      setInlineAmount('');
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });
  const [editRefused, setEditRefused] = useState<string | null>(null);
  const lock = useMutation({
    mutationFn: () => putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${c.id}/lock`, { by: user }),
    onSuccess: () => {
      onEdit();
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onError: (e) => setEditRefused(e instanceof HttpError && e.status === 403 ? VIEWER_REFUSED : EDIT_REFUSED),
  });
  const [selected, setSelected] = useSelectedClaims();
  const act = (action: Action) => {
    setEditRefused(null);
    lock.reset();
    reassign.reset();
    defer.reset();
    redate.reset();
    proof.reset();
    split.reset();
    remind.reset();
    comment.reset();
    line.reset();
    inline.reset();
    status.reset();
    status.mutate(action);
  };

  return (
    <li data-testid={`claim-row-${c.id}`}>
      <input
        type="checkbox"
        data-testid="claim-select"
        aria-label="Select claim"
        checked={selected.includes(c.id)}
        onChange={(e) => setSelected(e.target.checked ? [...selected, c.id] : selected.filter((id) => id !== c.id))}
      />{' '}
      <span data-testid="claim-number">{c.number ?? ''}</span>{' '}
      <span data-testid="claim-amount">{c.amount}</span> <span data-testid="claim-date">{c.date}</span>{' '}
      <span data-testid="claim-note">{c.note}</span>{' '}
      <span data-testid="claim-due">{claimDue(c, rate, floor, late)}</span>{' '}
      <span data-testid="claim-due-by">{dueBy(c)}</span>{' '}
      <span data-testid="claim-overdue">{isOverdue(c, late) ? 'yes' : 'no'}</span>{' '}
      <span data-testid="claim-status">{c.status}</span>{' '}
      <span data-testid="claim-opened-by">{c.openedBy ?? ''}</span>{' '}
      <span data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</span>{' '}
      <span data-testid="claim-corrects">{c.corrects ?? ''}</span>{' '}
      <span data-testid="claim-ref">{c.ref ?? ''}</span>{' '}
      <span data-testid="claim-tags">{c.tags.join(', ')}</span>{' '}
      {/* a claim voided while deferred still shows the deferral until its date passes (it counts in no total either way) */}
      <span data-testid="claim-deferred">
        {c.deferred || (c.status === 'VOID' && c.deferredUntil !== null && today !== '' && c.deferredUntil > today) ? 'yes' : 'no'}
      </span>{' '}
      <span data-testid="claim-deferred-until">{c.deferredUntil ?? ''}</span>{' '}
      <span data-testid="claim-proofs">{c.proofs}</span>{' '}
      <span data-testid="claim-split-from">{c.splitFrom ?? ''}</span>{' '}
      <span data-testid="claim-late-entry">{c.lateEntry ? 'yes' : 'no'}</span>{' '}
      <span data-testid="claim-reminded">{c.reminded ?? ''}</span>{' '}
      <span data-testid="claim-batch">{c.batch ?? ''}</span>{' '}
      <span data-testid="claim-period">{periodOf(periods.data, c.dueBy)?.name ?? ''}</span>{' '}
      <button
        type="button"
        data-testid="claim-edit"
        onClick={() => {
          status.reset();
          reassign.reset();
          defer.reset();
          redate.reset();
          proof.reset();
          split.reset();
          remind.reset();
          comment.reset();
          line.reset();
          inline.reset();
          lock.reset();
          setEditRefused(null);
          // refused at once when the screen already knows the claim cannot be edited; else the server decides
          if (c.status !== 'DRAFT' || periodOf(periods.data, c.dueBy)?.closed) {
            setEditRefused(DRAFT_ONLY);
            return;
          }
          if (viewer) {
            setEditRefused(VIEWER_REFUSED);
            return;
          }
          if (c.lockedBy && c.lockedBy !== user) {
            setEditRefused(LOCK_REFUSED);
            return;
          }
          lock.mutate();
        }}
        disabled={lock.isPending}
      >
        Edit
      </button>
      {editing && (
        <button type="button" data-testid="claim-cancel" onClick={onCancel}>
          Cancel
        </button>
      )}{' '}
      <input
        data-testid="claim-inline-amount"
        placeholder="Amount"
        inputMode="decimal"
        value={inlineAmount}
        onChange={(e) => {
          setInlineAmount(e.target.value);
          inline.reset();
        }}
      />
      <button
        type="button"
        data-testid="claim-inline-save"
        disabled={inline.isPending}
        onClick={() => {
          setEditRefused(null);
          lock.reset();
          status.reset();
          reassign.reset();
          defer.reset();
          redate.reset();
          proof.reset();
          split.reset();
          remind.reset();
          comment.reset();
          line.reset();
          inline.reset();
          inline.mutate(inlineAmount);
        }}
      >
        Save amount
      </button>{' '}
      <button type="button" data-testid="claim-open" disabled={status.isPending} onClick={() => act('open')}>
        Open
      </button>
      <button type="button" data-testid="claim-settle" disabled={status.isPending} onClick={() => act('settle')}>
        Settle
      </button>
      <button type="button" data-testid="claim-approve" disabled={status.isPending} onClick={() => act('approve')}>
        Approve
      </button>
      <button type="button" data-testid="claim-review" disabled={status.isPending} onClick={() => act('review')}>
        Review
      </button>
      <input
        data-testid="claim-reject-reason"
        placeholder="Reason"
        value={reason}
        onChange={(e) => {
          setReason(e.target.value);
          status.reset();
        }}
      />
      <button type="button" data-testid="claim-reject" disabled={status.isPending} onClick={() => act('reject')}>
        Reject
      </button>
      <select
        data-testid="claim-void-reason"
        aria-label="Void reason"
        value={voidReason}
        onChange={(e) => {
          setVoidReason(e.target.value);
          status.reset();
        }}
      >
        <option value="">(no reason)</option>
        {(voidReasons.data ?? []).map((r) => (
          <option key={r.reason} value={r.reason}>
            {r.reason}
          </option>
        ))}
      </select>
      <button type="button" data-testid="claim-void" disabled={status.isPending} onClick={() => act('void')}>
        Void
      </button>
      <button type="button" data-testid="claim-resettle" disabled={status.isPending} onClick={() => act('resettle')}>
        Re-settle
      </button>
      <button type="button" data-testid="claim-delete" disabled={status.isPending} onClick={() => act('delete')}>
        Delete
      </button>
      <button type="button" data-testid="claim-restore" disabled={status.isPending} onClick={() => act('restore')}>
        Restore
      </button>
      <button
        type="button"
        data-testid="claim-proof"
        disabled={proof.isPending}
        onClick={() => {
          setEditRefused(null);
          lock.reset();
          status.reset();
          reassign.reset();
          defer.reset();
          redate.reset();
          proof.reset();
          split.reset();
          remind.reset();
          comment.reset();
          line.reset();
          inline.reset();
          proof.mutate();
        }}
      >
        Add proof
      </button>
      <input
        type="date"
        data-testid="claim-defer-date"
        aria-label="Defer until"
        value={deferUntil}
        onChange={(e) => {
          setDeferUntil(e.target.value);
          defer.reset();
          redate.reset();
          proof.reset();
          split.reset();
          remind.reset();
          comment.reset();
          line.reset();
          inline.reset();
        }}
      />
      <button
        type="button"
        data-testid="claim-defer"
        disabled={defer.isPending}
        onClick={() => {
          setEditRefused(null);
          lock.reset();
          status.reset();
          reassign.reset();
          defer.reset();
          redate.reset();
          proof.reset();
          split.reset();
          remind.reset();
          comment.reset();
          line.reset();
          inline.reset();
          defer.mutate(deferUntil.trim());
        }}
      >
        Defer
      </button>{' '}
      <input
        type="date"
        data-testid="claim-date-new"
        aria-label="New date"
        value={newDate}
        onChange={(e) => {
          setNewDate(e.target.value);
          redate.reset();
        }}
      />
      <button
        type="button"
        data-testid="claim-redate"
        disabled={redate.isPending}
        onClick={() => {
          setEditRefused(null);
          lock.reset();
          status.reset();
          reassign.reset();
          defer.reset();
          redate.reset();
          proof.reset();
          split.reset();
          remind.reset();
          comment.reset();
          line.reset();
          inline.reset();
          redate.mutate(newDate.trim());
        }}
      >
        Re-date
      </button>{' '}
      <input
        data-testid="claim-split-amount"
        placeholder="Split amount"
        inputMode="decimal"
        value={splitAmount}
        onChange={(e) => {
          setSplitAmount(e.target.value);
          split.reset();
          remind.reset();
          comment.reset();
          line.reset();
          inline.reset();
        }}
      />
      <button
        type="button"
        data-testid="claim-split"
        disabled={split.isPending}
        onClick={() => {
          setEditRefused(null);
          lock.reset();
          status.reset();
          reassign.reset();
          defer.reset();
          redate.reset();
          proof.reset();
          split.reset();
          remind.reset();
          comment.reset();
          line.reset();
          inline.reset();
          split.mutate(splitAmount.trim());
        }}
      >
        Split
      </button>{' '}
      <select
        data-testid="claim-reassign-to"
        aria-label="Move to"
        value={moveTo}
        onChange={(e) => {
          setMoveTo(e.target.value);
          reassign.reset();
        }}
      >
        <option value="">Move to…</option>
        {(people.data ?? [])
          .filter((p) => p.id !== c.personId)
          .map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
      </select>
      <button
        type="button"
        data-testid="claim-reassign"
        disabled={reassign.isPending || moveTo === ''}
        onClick={() => {
          setEditRefused(null);
          lock.reset();
          status.reset();
          reassign.reset();
          defer.reset();
          redate.reset();
          proof.reset();
          split.reset();
          remind.reset();
          comment.reset();
          line.reset();
          inline.reset();
          reassign.mutate(Number(moveTo));
        }}
      >
        Move
      </button>{' '}
      <button
        type="button"
        data-testid="claim-remind"
        disabled={remind.isPending}
        onClick={() => {
          setEditRefused(null);
          lock.reset();
          status.reset();
          reassign.reset();
          defer.reset();
          redate.reset();
          proof.reset();
          split.reset();
          remind.reset();
          comment.reset();
          line.reset();
          inline.reset();
          remind.mutate();
        }}
      >
        Remind
      </button>{' '}
      <input
        data-testid="claim-comment-text"
        placeholder="Comment"
        value={commentText}
        onChange={(e) => {
          setCommentText(e.target.value);
          comment.reset();
          line.reset();
        }}
      />
      <button
        type="button"
        data-testid="claim-comment-add"
        disabled={comment.isPending}
        onClick={() => {
          setEditRefused(null);
          lock.reset();
          status.reset();
          reassign.reset();
          defer.reset();
          redate.reset();
          proof.reset();
          split.reset();
          remind.reset();
          comment.reset();
          line.reset();
          inline.reset();
          // an empty comment is refused at once; else the server decides
          if (commentText.trim() === '') {
            setEditRefused(EMPTY_COMMENT);
            return;
          }
          comment.mutate(commentText);
        }}
      >
        Comment
      </button>
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
        disabled={line.isPending}
        onClick={() => {
          setEditRefused(null);
          lock.reset();
          status.reset();
          reassign.reset();
          defer.reset();
          redate.reset();
          proof.reset();
          split.reset();
          remind.reset();
          comment.reset();
          line.reset();
          inline.reset();
          line.mutate({ label: lineLabel, qty: lineQty, unit: lineUnit });
        }}
      >
        Add line
      </button>{' '}
      {editRefused !== null && <span data-testid="claim-error"> {editRefused}</span>}
      {status.isError && (
        <span data-testid="claim-error">
          {' '}
          {(c.status === 'OPEN' || c.status === 'PENDING') && c.openedBy === user && status.variables !== 'reject' && status.variables !== 'void' && status.variables !== 'resettle'
            ? 'You opened this claim, so someone else must settle or approve it.'
            : `A ${c.status} claim cannot be changed that way, nor one whose due-by is inside a closed period, nor a deferred one settled, nor one with no proofs settled or approved when its due is above the proof threshold, nor one with an empty note opened when its due is above the note threshold; the user who opened or last edited a claim cannot settle or approve it, only a steward approves or rejects a pending claim or re-settles a settled one, only a steward voids a settled claim and only within 7 days of its settlement, a void needs a reason from the sheet's list while it has any, a rejection needs a reason, only a draft is deleted and a deleted claim is restored only on the sheet-today it was deleted, and only a role the sheet lets open, settle or void a claim may do so.`}
        </span>
      )}
      {reassign.isError && (
        <span data-testid="claim-error">
          {' '}
          Only a role the sheet lets reassign (a steward unless set) moves a claim, and only a draft, open or pending one (not a correction, nor one whose due-by is inside a
          closed period) to another person not retired before its date.
        </span>
      )}
      {defer.isError && (
        <span data-testid="claim-error">
          {' '}
          Only an open claim whose due-by is not inside a closed period can be deferred, and only to a date after today.
        </span>
      )}
      {redate.isError && (
        <span data-testid="claim-error">
          {' '}
          Only a steward re-dates a claim, and only an open or pending one whose due-by is not inside a closed period, to a date
          not after the person's retirement.
        </span>
      )}
      {proof.isError && (
        <span data-testid="claim-error"> A settled or void claim, or one whose due-by is inside a closed period, takes no more proofs.</span>
      )}
      {split.isError && (
        <span data-testid="claim-error">
          {' '}
          Only an open claim, not deferred, nor with its due-by inside a closed period, can be split, by an amount more than 0.00 and less than
          its amount, and only by a role the sheet lets open a claim.
        </span>
      )}
      {remind.isError && (
        <span data-testid="claim-error">
          {' '}
          Only an open or pending claim can be reminded, and only once a day.
        </span>
      )}
      {comment.isError && (
        <span data-testid="claim-error">
          {' '}
          {comment.error instanceof HttpError && comment.error.status === 403 ? VIEWER_REFUSED : EMPTY_COMMENT}
        </span>
      )}
      {line.isError && (
        <span data-testid="claim-error">
          {' '}
          {line.error instanceof HttpError && line.error.status === 403
            ? VIEWER_REFUSED
            : 'Only a draft claim whose due-by is not inside a closed period, and that no one else is editing, takes a line: a label, a whole quantity of at least 1 and a unit amount with up to two decimals.'}
        </span>
      )}
      {inline.isError && (
        <span data-testid="claim-error">
          {' '}
          {inline.error instanceof HttpError && inline.error.status === 403
            ? VIEWER_REFUSED
            : 'Only a draft claim whose due-by is not inside a closed period, that no one else is editing and that has no lines, can have its amount changed, to one with up to two decimals, 0.00 or more unless it corrects a settled claim.'}
        </span>
      )}
      <ol data-testid="claim-log">
        {c.log.map((l, i) => (
          <li key={i} data-testid="claim-log-line">
            {logLine(l)}
          </li>
        ))}
      </ol>
      <ol data-testid="claim-lines">
        {c.lines.map((l, i) => (
          <li key={i} data-testid="claim-lines-line">
            {lineLine(l)}
          </li>
        ))}
      </ol>
      <ol data-testid="claim-comments">
        {c.comments.map((m, i) => (
          <li key={i} data-testid="claim-comments-line">
            {commentLine(m)}
          </li>
        ))}
      </ol>
    </li>
  );
}

/** A person's claimed, and their claims (newest first) with the form that adds one or saves the one
 *  being edited. A blank date takes the sheet's entry date, else its today (the server applies it). */
function PersonClaims({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId).data;
  const floor = useFloor(sheetId);
  const today = useSheetToday(sheetId);
  const queryClient = useQueryClient();
  const mine = claims.data?.filter((c) => c.personId === person.id) ?? [];
  const [editingId, setEditingId] = useState<number | undefined>(undefined);
  const [values, setValues] = useState<Values>(EMPTY);
  const unlock = useMutation({
    mutationFn: (id: number) => putJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/${id}/unlock`, { by: user }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
  });

  const submit = useSubmitOnce();
  const save = useMutation({
    mutationFn: ({ id, ...v }: Values & { id?: number }) => {
      const base = `/api/sheets/${encodeURIComponent(sheetId)}`;
      const body = { ...v, by: user };
      return id === undefined
        ? postJson<Claim>(`${base}/people/${person.id}/claims`, body)
        : putJson<Claim>(`${base}/claims/${id}`, body);
    },
    onSuccess: () => {
      setValues(EMPTY);
      setEditingId(undefined);
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onSettled: submit.end,
  });

  // A DRAFT claim with the chosen template's amount and note as they are now, no date: added by the claim form's rules,
  // its refusal shown in the form's error.
  const templates = useTemplates(sheetId).data ?? [];
  const [template, setTemplate] = useState('');
  const fromTemplate = useMutation({
    mutationFn: (name: string) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/template-claims`, { template: name, by: user }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
    onSettled: submit.end,
  });

  const change = (v: Partial<Values>) => {
    setValues((was) => ({ ...was, ...v }));
    save.reset();
    fromTemplate.reset();
  };
  const openEditor = (c: Claim) => {
    save.reset();
    fromTemplate.reset();
    if (editingId !== undefined && editingId !== c.id) unlock.mutate(editingId);
    setValues({ amount: c.amount, date: c.date, note: c.note, corrects: c.corrects ?? '', ref: c.ref ?? '', tags: c.tags.join(', ') });
    setEditingId(c.id);
  };
  const cancel = () => {
    save.reset();
    fromTemplate.reset();
    if (editingId !== undefined) unlock.mutate(editingId);
    setValues(EMPTY);
    setEditingId(undefined);
  };

  // The editor of a claim the user still holds (its lock outlived a reload of the sheet) opens again once the claims
  // are read fresh, so it can be saved or cancelled.
  const restored = useRef(false);
  useEffect(() => {
    if (restored.current || claims.data === undefined || claims.isFetching) return;
    restored.current = true;
    const held = mine.find((c) => c.lockedBy === user && c.status === 'DRAFT');
    if (held !== undefined && editingId === undefined) openEditor(held);
  });

  return (
    <>
      <td data-testid="person-claimed">{claimed(live(mine, [person], today))}</td>
      <td>
        <form
          data-testid="claim-form"
          onSubmit={(e) => {
            e.preventDefault();
            if (!submit.begin()) return;
            save.reset();
            fromTemplate.reset();
            save.mutate({ ...values, id: editingId });
          }}
        >
          <input
            data-testid="claim-form-amount"
            placeholder="Amount"
            inputMode="decimal"
            value={values.amount}
            onChange={(e) => change({ amount: e.target.value })}
          />{' '}
          <input type="date" data-testid="claim-form-date" value={values.date} onChange={(e) => change({ date: e.target.value })} />{' '}
          <input data-testid="claim-form-note" placeholder="Note" value={values.note} onChange={(e) => change({ note: e.target.value })} />{' '}
          <input
            data-testid="claim-form-corrects"
            placeholder="Corrects no."
            inputMode="numeric"
            value={values.corrects}
            onChange={(e) => change({ corrects: e.target.value })}
          />{' '}
          <input data-testid="claim-form-ref" placeholder="Reference" value={values.ref} onChange={(e) => change({ ref: e.target.value })} />{' '}
          <input data-testid="claim-form-tags" placeholder="Tags, comma separated" value={values.tags} onChange={(e) => change({ tags: e.target.value })} />{' '}
          <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
            {editingId === undefined ? 'Add claim' : 'Save claim'}
          </button>
          <select
            data-testid="claim-template-select"
            value={template}
            onChange={(e) => {
              setTemplate(e.target.value);
              save.reset();
              fromTemplate.reset();
            }}
          >
            <option value="">Template…</option>
            {templates.map((t) => (
              <option key={t.name} value={t.name}>
                {t.name}
              </option>
            ))}
          </select>{' '}
          <button
            type="button"
            data-testid="claim-template-add"
            disabled={fromTemplate.isPending}
            onClick={() => {
              if (!submit.begin()) return;
              save.reset();
              fromTemplate.reset();
              fromTemplate.mutate(template);
            }}
          >
            Add from template
          </button>
          {(save.isError || fromTemplate.isError) && (
            <span data-testid="claim-form-error">
              The amount must have up to two decimals, and be 0.00 or more unless it corrects, by number, a settled claim of this person; its due-by not inside a closed period, the date not after the person's retirement date, the reference not one already used on the sheet, and only a draft claim can be edited.
            </span>
          )}
        </form>
        <ul data-testid="claim-list">
          {mine.map((c) => (
            <ClaimRow
              key={c.id}
              sheetId={sheetId}
              user={user}
              claim={c}
              rate={rateFor(rates, gradeOn(person, c.date), c.date)}
              floor={floor}
              editing={editingId === c.id}
              onEdit={() => openEditor(c)}
              onCancel={cancel}
            />
          ))}
        </ul>
      </td>
    </>
  );
}

/** The sheet's claimed-total: everyone's claimed (OPEN, PENDING and SETTLED claims that correct no other, but no OPEN or PENDING ones dated within a hold of their person, or of one whose retirement date is reached, and not bound by a statement) added up. */
function ClaimedTotal({ sheetId }: { sheetId: string }) {
  const claims = useClaims(sheetId);
  const people = usePeople(sheetId);
  const today = useSheetToday(sheetId);
  return (
    <p>
      Claimed total <span data-testid="rollup-claimed-total">{claimed(live(claims.data ?? [], people.data, today))}</span>
      {claims.isError && <span data-testid="claims-load-error"> Could not load claims.</span>}
    </p>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 10, Component: ClaimsHead }),
  PersonRow.fill({ order: 10, Component: PersonClaims }),
  SheetPage.fill({ order: 20, Component: ClaimedTotal }),
];
