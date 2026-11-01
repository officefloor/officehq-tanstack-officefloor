import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { gradeOn, peoplePath, usePeople } from '../people/people';
import type { Grade, Person } from '../people/people';
import { useSheetToday } from '../sheet/sheet';
import { rateOf, showDue, twoDecimals, useRates } from '../rates/rates';
import type { Rate } from '../rates/rates';
import { periodOf, usePeriods } from '../periods/periods';
import { useCapCents, useFloorCents } from '../settings/settings';
import {
  claimDueCents,
  claimed,
  claimsKey,
  claimsPath,
  counted,
  overdue,
  overdueCents,
  agedCents,
  splitDueCents,
  personDueCents,
  uncappedPersonDueCents,
  useClaims,
  useLate,
} from './claims';
import type { Claim, ClaimStatusChange } from './claims';
import { selectClaim, useSelectedClaims } from './selection';
import { useVoidReasons, voidReasonsKey } from '../voidreasons/voidreasons';

type Draft = { amount: string; date: string; note: string; corrects: string; ref: string; tags: string };
const EMPTY: Draft = { amount: '', date: '', note: '', corrects: '', ref: '', tags: '' };

function ClaimedHeader() {
  return (
    <>
      <th>Claimed</th>
      <th>Due total</th>
      <th>Excess</th>
      <th>Overdue</th>
      <th>Overdue total</th>
      <th>Due 0-30 days</th>
      <th>Due 31-60 days</th>
      <th>Due 61+ days</th>
      <th>Settled total</th>
      <th>Open total</th>
      <th>Claims</th>
    </>
  );
}

const CHANGES: { change: ClaimStatusChange; label: string }[] = [
  { change: 'open', label: 'Open' },
  { change: 'settle', label: 'Settle' },
  { change: 'void', label: 'Void' },
  { change: 'approve', label: 'Approve' },
];

// One claim with its status and the buttons that change it. Every button is offered; the server
// refuses a change the claim's status does not allow, and the row shows that error until its next action.
function ClaimRow({ sheet, user, claim, rates, grade, editing, onCancel }: {
  sheet: string;
  user: string;
  claim: Claim;
  rates: Rate[];
  grade: Grade | undefined;
  editing: boolean;
  onCancel: () => void;
}) {
  const queryClient = useQueryClient();
  const { data: periods = [] } = usePeriods(sheet);
  const floor = useFloorCents(sheet);
  const today = useSheetToday(sheet);
  const late = useLate(sheet);
  // While the sheet has void reasons, a void needs one, chosen beside the Void button.
  const { data: voidReasons = [] } = useVoidReasons(sheet);
  const [voidReason, setVoidReason] = useState('');
  const status = useMutation({
    mutationFn: (change: ClaimStatusChange) =>
      postJson<Claim[]>(
        byUser(`${claimsPath(sheet)}/${claim.id}/${change}`, user),
        change === 'reject' ? { reason } : change === 'void' ? { reason: voidReason } : {},
      ),
    onSuccess: (updated, change) => {
      queryClient.setQueryData(claimsKey(sheet), updated);
      // A void counts against the reason it was given, in the summary.
      if (change === 'void') void queryClient.invalidateQueries({ queryKey: voidReasonsKey(sheet) });
    },
  });

  const act = useSubmitOnce(status);
  // A role the permission matrix lets (by default a steward) moves a DRAFT, OPEN or PENDING claim to another person on the sheet, picked beside the Reassign button.
  const { data: people = [] } = usePeople(sheet);
  const [moveTo, setMoveTo] = useState('');
  const move = useMutation({
    mutationFn: (to: number) =>
      postJson<Claim[]>(byUser(`${claimsPath(sheet)}/${claim.id}/reassign`, user), { to }),
    onSuccess: (updated) => {
      setMoveTo('');
      queryClient.setQueryData(claimsKey(sheet), updated);
    },
  });
  const reassign = useSubmitOnce(move);
  // A steward re-settles a SETTLED claim: its due is fixed again at today's live figure.
  const resettleMutation = useMutation({
    mutationFn: () => postJson<Claim[]>(byUser(`${claimsPath(sheet)}/${claim.id}/resettle`, user), {}),
    onSuccess: (updated) => queryClient.setQueryData(claimsKey(sheet), updated),
  });
  const resettle = useSubmitOnce(resettleMutation);
  // An OPEN claim is deferred to a date after the sheet's today, typed beside the Defer button.
  const [deferTo, setDeferTo] = useState('');
  const deferMutation = useMutation({
    mutationFn: (until: string) => postJson<Claim[]>(byUser(`${claimsPath(sheet)}/${claim.id}/defer`, user), { until }),
    onSuccess: (updated) => {
      setDeferTo('');
      queryClient.setQueryData(claimsKey(sheet), updated);
    },
  });
  const defer = useSubmitOnce(deferMutation);
  // An OPEN or PENDING claim is reminded, at most once per sheet-today.
  const remindMutation = useMutation({
    mutationFn: () => postJson<Claim[]>(byUser(`${claimsPath(sheet)}/${claim.id}/remind`, user), {}),
    onSuccess: (updated) => queryClient.setQueryData(claimsKey(sheet), updated),
  });
  const remind = useSubmitOnce(remindMutation);
  // A steward re-dates an OPEN or PENDING claim to a date typed beside the Re-date button.
  const [redateTo, setRedateTo] = useState('');
  const redateMutation = useMutation({
    mutationFn: (date: string) => postJson<Claim[]>(byUser(`${claimsPath(sheet)}/${claim.id}/redate`, user), { date }),
    onSuccess: (updated) => {
      setRedateTo('');
      queryClient.setQueryData(claimsKey(sheet), updated);
    },
  });
  const redate = useSubmitOnce(redateMutation);
  // A proof raises the claim's proofs count by one (not on a SETTLED or VOID claim, nor one in a closed period).
  const proofMutation = useMutation({
    mutationFn: () => postJson<Claim[]>(byUser(`${claimsPath(sheet)}/${claim.id}/proof`, user), {}),
    onSuccess: (updated) => queryClient.setQueryData(claimsKey(sheet), updated),
  });
  const addProof = useSubmitOnce(proofMutation);
  // A comment, typed beside the Comment button, is kept with its writer and the sheet's today (an empty one is refused).
  const [commentText, setCommentText] = useState('');
  const commentMutation = useMutation({
    mutationFn: (text: string) => postJson<Claim[]>(byUser(`${claimsPath(sheet)}/${claim.id}/comment`, user), { text }),
    onSuccess: (updated) => {
      setCommentText('');
      queryClient.setQueryData(claimsKey(sheet), updated);
    },
  });
  const addComment = useSubmitOnce(commentMutation);
  // An OPEN claim splits off an amount, typed beside the Split button, into a new OPEN claim.
  const [splitAmount, setSplitAmount] = useState('');
  const splitMutation = useMutation({
    mutationFn: (amount: string) => postJson<Claim[]>(byUser(`${claimsPath(sheet)}/${claim.id}/split`, user), { amount }),
    onSuccess: (updated) => {
      setSplitAmount('');
      queryClient.setQueryData(claimsKey(sheet), updated);
    },
  });
  const split = useSubmitOnce(splitMutation);
  const selected = useSelectedClaims(sheet).has(claim.id);
  // Edit opens the claim's editor by locking it for this user: the server refuses a claim that is not a DRAFT, one
  // due inside a closed period, or one another user's editor holds, and the row shows the error instead.
  const lockMutation = useMutation({
    mutationFn: () => postJson<Claim[]>(byUser(`${claimsPath(sheet)}/${claim.id}/lock`, user), {}),
    onSuccess: (updated) => queryClient.setQueryData(claimsKey(sheet), updated),
  });
  const lock = useSubmitOnce(lockMutation);
  // What the screen already knows refuses at once (the server refuses the rest): a claim that is not a DRAFT, one
  // due inside a closed period, or one another user's editor holds.
  const [editRefused, setEditRefused] = useState(false);
  const clearEditError = () => {
    setEditRefused(false);
    lockMutation.reset();
  };
  // A DRAFT's amount is changed in the row: saved as the claim's editor saves it (its date, note, corrects, reference and tags kept),
  // so the server refuses it by the same rules (the amount's form, a claim that is not a DRAFT, one due inside a
  // closed period, one another user's editor holds, a viewer).
  const [inlineAmount, setInlineAmount] = useState('');
  const inlineMutation = useMutation({
    mutationFn: (amount: string) =>
      putJson<Claim[]>(byUser(`${claimsPath(sheet)}/${claim.id}`, user), {
        amount,
        date: claim.date,
        note: claim.note,
        corrects: claim.corrects ?? '',
        ref: claim.ref ?? '',
        tags: claim.tags.join(','),
      }),
    onSuccess: (updated) => {
      setInlineAmount('');
      queryClient.setQueryData(claimsKey(sheet), updated);
    },
  });
  const saveInline = useSubmitOnce(inlineMutation);
  // A steward rejects a PENDING claim with a reason, typed beside the Reject button.
  const [reason, setReason] = useState('');

  return (
    <li data-testid={`claim-row-${claim.id}`}>
      <input
        type="checkbox"
        data-testid="claim-select"
        checked={selected}
        onChange={(e) => selectClaim(sheet, claim.id, e.target.checked)}
      />{' '}
      <span data-testid="claim-amount">{claim.amount}</span>{' '}
      <span data-testid="claim-date">{claim.date}</span>{' '}
      <span data-testid="claim-note">{claim.note}</span>{' '}
      due <span data-testid="claim-due">{showDue(claimDueCents(claim, rateOf(rates, grade, claim.date), floor, late))}</span>{' '}
      due by <span data-testid="claim-due-by">{claim.dueBy}</span>{' '}
      overdue <span data-testid="claim-overdue">{overdue(claim, late) ? 'yes' : 'no'}</span>{' '}
      <span data-testid="claim-status">{claim.status}</span>{' '}
      stated <span data-testid="claim-stated">{claim.stated ? 'yes' : 'no'}</span>{' '}
      period <span data-testid="claim-period">{periodOf(periods, claim.dueBy)?.name ?? ''}</span>{' '}
      {(claim.periodNumber ?? claim.number) !== null && '#'}
      <span data-testid="claim-number">{claim.periodNumber ?? claim.number ?? ''}</span>{' '}
      {claim.openedBy !== null && 'opened by '}
      <span data-testid="claim-opened-by">{claim.openedBy ?? ''}</span>{' '}
      {claim.corrects !== null && 'corrects #'}
      <span data-testid="claim-corrects">{claim.corrects ?? ''}</span>{' '}
      {claim.ref !== null && 'ref '}
      <span data-testid="claim-ref">{claim.ref ?? ''}</span>{' '}
      {claim.batch !== null && 'batch '}
      <span data-testid="claim-batch">{claim.batch ?? ''}</span>{' '}
      {claim.tags.length > 0 && 'tags '}
      <span data-testid="claim-tags">{claim.tags.join(', ')}</span>{' '}
      deferred <span data-testid="claim-deferred">{claim.deferred ? 'yes' : 'no'}</span>{' '}
      {claim.deferredUntil !== null && (claim.deferred ? 'until ' : 'last until ')}
      <span data-testid="claim-deferred-until">{claim.deferredUntil ?? ''}</span>{' '}
      {claim.splitFrom !== null && 'split from #'}
      <span data-testid="claim-split-from">{claim.splitFrom ?? ''}</span>{' '}
      reminded <span data-testid="claim-reminded">{claim.remindedOn ?? ''}</span>{' '}
      late entry <span data-testid="claim-late-entry">{claim.lateEntry ? 'yes' : 'no'}</span>{' '}
      proofs <span data-testid="claim-proofs">{claim.proofs}</span>{' '}
      <button
        type="button"
        data-testid="claim-proof"
        disabled={proofMutation.isPending}
        onClick={() => {
          clearEditError();
          status.reset();
          move.reset();
          resettleMutation.reset();
          deferMutation.reset();
          remindMutation.reset();
          redateMutation.reset();
          splitMutation.reset();
          inlineMutation.reset();
          addProof(undefined);
        }}
      >
        Add proof
      </button>{' '}
      {CHANGES.map(({ change, label }) => (
        <button
          key={change}
          type="button"
          data-testid={`claim-${change}`}
          disabled={status.isPending}
          onClick={() => {
            clearEditError();
            move.reset();
            resettleMutation.reset();
            deferMutation.reset();
          remindMutation.reset();
            redateMutation.reset();
            splitMutation.reset();
            inlineMutation.reset();
          inlineMutation.reset();
            proofMutation.reset();
            commentMutation.reset();
            act(change);
          }}
        >
          {label}
        </button>
      ))}{' '}
      <select
        data-testid="claim-void-reason"
        value={voidReason}
        onChange={(e) => {
          setVoidReason(e.target.value);
          status.reset();
        }}
      >
        <option value="">(void reason)</option>
        {voidReasons.map((r) => (
          <option key={r.reason} value={r.reason}>
            {r.reason}
          </option>
        ))}
      </select>{' '}
      <input
        data-testid="claim-reject-reason"
        placeholder="reject reason"
        value={reason}
        onChange={(e) => {
          setReason(e.target.value);
          status.reset();
          move.reset();
          splitMutation.reset();
          inlineMutation.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="claim-reject"
        disabled={status.isPending}
        onClick={() => {
          clearEditError();
          move.reset();
          resettleMutation.reset();
          deferMutation.reset();
          remindMutation.reset();
          redateMutation.reset();
          splitMutation.reset();
          inlineMutation.reset();
          proofMutation.reset();
          commentMutation.reset();
          act('reject');
        }}
      >
        Reject
      </button>{' '}
      <input
        data-testid="claim-inline-amount"
        placeholder="amount"
        value={inlineAmount}
        onChange={(e) => {
          setInlineAmount(e.target.value);
          inlineMutation.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="claim-inline-save"
        disabled={inlineMutation.isPending}
        onClick={() => {
          clearEditError();
          status.reset();
          move.reset();
          resettleMutation.reset();
          deferMutation.reset();
          remindMutation.reset();
          redateMutation.reset();
          splitMutation.reset();
          proofMutation.reset();
          commentMutation.reset();
          inlineMutation.reset();
          saveInline(inlineAmount);
        }}
      >
        Save amount
      </button>{' '}
      <button
        type="button"
        data-testid="claim-edit"
        onClick={() => {
          status.reset();
          move.reset();
          resettleMutation.reset();
          deferMutation.reset();
          remindMutation.reset();
          redateMutation.reset();
          splitMutation.reset();
          inlineMutation.reset();
          proofMutation.reset();
          commentMutation.reset();
          clearEditError();
          if (claim.status !== 'DRAFT' || periodOf(periods, claim.dueBy)?.closed || (claim.lockedBy !== null && claim.lockedBy !== user)) {
            setEditRefused(true);
            return;
          }
          lock(undefined);
        }}
      >
        Edit
      </button>
      <select
        data-testid="claim-reassign-to"
        value={moveTo}
        onChange={(e) => {
          setMoveTo(e.target.value);
          move.reset();
          splitMutation.reset();
          inlineMutation.reset();
        }}
      >
        <option value="">move to…</option>
        {people
          .filter((p) => p.id !== claim.person)
          .map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
      </select>{' '}
      <button
        type="button"
        data-testid="claim-reassign"
        disabled={move.isPending || moveTo === ''}
        onClick={() => {
          clearEditError();
          status.reset();
          resettleMutation.reset();
          deferMutation.reset();
          remindMutation.reset();
          redateMutation.reset();
          splitMutation.reset();
          inlineMutation.reset();
          proofMutation.reset();
          commentMutation.reset();
          reassign(Number(moveTo));
        }}
      >
        Reassign
      </button>{' '}
      <button
        type="button"
        data-testid="claim-resettle"
        disabled={resettleMutation.isPending}
        onClick={() => {
          clearEditError();
          status.reset();
          move.reset();
          deferMutation.reset();
          remindMutation.reset();
          redateMutation.reset();
          splitMutation.reset();
          inlineMutation.reset();
          proofMutation.reset();
          commentMutation.reset();
          resettle(undefined);
        }}
      >
        Re-settle
      </button>{' '}
      <input
        data-testid="claim-defer-date"
        placeholder="defer until yyyy-mm-dd"
        value={deferTo}
        onChange={(e) => {
          setDeferTo(e.target.value);
          deferMutation.reset();
          remindMutation.reset();
          redateMutation.reset();
          splitMutation.reset();
          inlineMutation.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="claim-defer"
        disabled={deferMutation.isPending}
        onClick={() => {
          clearEditError();
          status.reset();
          move.reset();
          resettleMutation.reset();
          proofMutation.reset();
          commentMutation.reset();
          splitMutation.reset();
          inlineMutation.reset();
          redateMutation.reset();
          remindMutation.reset();
          defer(deferTo);
        }}
      >
        Defer
      </button>{' '}
      <button
        type="button"
        data-testid="claim-remind"
        disabled={remindMutation.isPending}
        onClick={() => {
          clearEditError();
          status.reset();
          move.reset();
          resettleMutation.reset();
          deferMutation.reset();
          redateMutation.reset();
          proofMutation.reset();
          commentMutation.reset();
          splitMutation.reset();
          inlineMutation.reset();
          remindMutation.reset();
          remind(undefined);
        }}
      >
        Remind
      </button>{' '}
      <input
        data-testid="claim-date-new"
        placeholder="re-date yyyy-mm-dd"
        value={redateTo}
        onChange={(e) => {
          setRedateTo(e.target.value);
          redateMutation.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="claim-redate"
        disabled={redateMutation.isPending}
        onClick={() => {
          clearEditError();
          status.reset();
          move.reset();
          resettleMutation.reset();
          deferMutation.reset();
          remindMutation.reset();
          proofMutation.reset();
          commentMutation.reset();
          splitMutation.reset();
          inlineMutation.reset();
          redate(redateTo);
        }}
      >
        Re-date
      </button>{' '}
      <input
        data-testid="claim-split-amount"
        placeholder="split off amount"
        value={splitAmount}
        onChange={(e) => {
          setSplitAmount(e.target.value);
          splitMutation.reset();
          inlineMutation.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="claim-split"
        disabled={splitMutation.isPending}
        onClick={() => {
          clearEditError();
          status.reset();
          move.reset();
          resettleMutation.reset();
          deferMutation.reset();
          remindMutation.reset();
          redateMutation.reset();
          proofMutation.reset();
          commentMutation.reset();
          split(splitAmount);
        }}
      >
        Split
      </button>{' '}
      <input
        data-testid="claim-comment-text"
        placeholder="comment"
        value={commentText}
        onChange={(e) => {
          setCommentText(e.target.value);
          commentMutation.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="claim-comment-add"
        disabled={commentMutation.isPending}
        onClick={() => {
          clearEditError();
          status.reset();
          move.reset();
          resettleMutation.reset();
          deferMutation.reset();
          remindMutation.reset();
          redateMutation.reset();
          proofMutation.reset();
          splitMutation.reset();
          inlineMutation.reset();
          commentMutation.reset();
          addComment(commentText);
        }}
      >
        Comment
      </button>{' '}
      {editing && (
        <button type="button" data-testid="claim-cancel" onClick={onCancel}>
          Cancel
        </button>
      )}
      <ol>
        {claim.log.map((line, i) => (
          <li key={i} data-testid="claim-log-line">
            {line.status} by {line.by} on {line.on} due={line.due}
          </li>
        ))}
      </ol>
      <ul>
        {claim.comments.map((comment, i) => (
          <li key={i} data-testid="claim-comments-line">
            {comment.by} on {comment.on}: {comment.text}
          </li>
        ))}
      </ul>
      {(editRefused || lockMutation.isError) && (
        <span data-testid="claim-error" role="alert">
          {' '}This claim cannot be edited: only a DRAFT outside a closed period can (void it and add a new one), and
          not while another user has its editor open.
        </span>
      )}
      {status.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}A {claim.status} claim cannot be changed that way (your role may not take that action; the user who opened it cannot settle or approve it; only a steward approves or rejects a PENDING claim, a reject needs a reason, a void needs a reason from the sheet's list while it has one, a claim whose due is above the proof threshold needs a proof, and one whose due is above the note threshold needs a note to be opened).
        </span>
      )}
      {resettleMutation.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}A {claim.status} claim cannot be re-settled (only a steward re-settles, and only a SETTLED claim outside a closed period).
        </span>
      )}
      {deferMutation.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}A {claim.status} claim cannot be deferred (only an OPEN claim outside a closed period, and only to a date after the sheet's today).
        </span>
      )}
      {remindMutation.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}A {claim.status} claim cannot be reminded (only an OPEN or PENDING claim, and only once per sheet-today).
        </span>
      )}
      {redateMutation.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}A {claim.status} claim cannot be re-dated (only a steward re-dates, only an OPEN or PENDING claim outside a closed period, and only to a date outside a closed period, on or before its person's retirement).
        </span>
      )}
      {proofMutation.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}A {claim.status} claim cannot be given a proof (not a SETTLED or VOID claim, nor one due inside a closed period).
        </span>
      )}
      {commentMutation.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}This comment cannot be added: it may not be empty, and a viewer, an archived sheet or the as-of view changes nothing.
        </span>
      )}
      {splitMutation.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}A {claim.status} claim cannot be split (only an OPEN claim, not deferred nor due inside a closed period, by a role that may open a claim, and only an amount more than 0.00 and less than the claim's).
        </span>
      )}
      {inlineMutation.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}This amount cannot be saved: it needs up to two decimals (a negative one must be a correction), and only a DRAFT outside a closed period can be edited, not while another user has its editor open, nor by a viewer.
        </span>
      )}
      {move.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}A {claim.status} claim cannot be reassigned (only a role the permission matrix lets, by default a steward, moves a claim, and only a DRAFT, OPEN or PENDING one, to another person).
        </span>
      )}
    </li>
  );
}

// A person's claimed total, then their claims (newest first) under one form that adds a claim or,
// once a claim's editor is opened, edits it. A blank date takes the sheet's today on the server.
function PersonClaims({ sheet, user, person }: { sheet: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const { data: all = [] } = useClaims(sheet);
  const claims = all.filter((c) => c.person === person.id);
  const { data: rates = [] } = useRates(sheet);
  const floor = useFloorCents(sheet);
  const cap = useCapCents(sheet);
  const today = useSheetToday(sheet);
  const late = useLate(sheet);
  // The excess: how far the sum of their figures is above the cap (0.00 when it is not, or is n/a).
  const uncapped = uncappedPersonDueCents(claims, rates, person, today, floor, late);
  const excess = uncapped !== undefined && cap !== undefined && uncapped > cap ? uncapped - cap : 0n;
  const lateClaims = overdueCents(claims, [person], rates, late, floor);
  const aged = agedCents(claims, person, rates, late, floor);
  const split = splitDueCents(claims, rates, person, floor, late);
  const [draft, setDraft] = useState<Draft>(EMPTY);
  // The editor is open on the claim this user's lock holds, as the store says: it stays open across a re-load of the
  // sheet, and closes once the lock is released (saved, cancelled, or the sheet's today changed).
  const locked = claims.find((c) => c.lockedBy === user);
  const editing = locked?.id ?? null;
  const save = useMutation({
    mutationFn: ({ id, body }: { id: number | null; body: Draft }) =>
      id === null
        ? postJson<Claim[]>(byUser(`${peoplePath(sheet)}/${person.id}/claims`, user), body)
        : putJson<Claim[]>(byUser(`${claimsPath(sheet)}/${id}`, user), body),
    onSuccess: (updated) => {
      queryClient.setQueryData(claimsKey(sheet), updated);
      setDraft(EMPTY);
    },
  });
  const send = useSubmitOnce(save);

  const filled = useRef<number | null>(null);
  useEffect(() => {
    if (filled.current === editing) return;
    filled.current = editing;
    setDraft(
      locked === undefined
        ? EMPTY
        : { amount: locked.amount, date: locked.date, note: locked.note, corrects: locked.corrects ?? '', ref: locked.ref ?? '', tags: locked.tags.join(', ') },
    );
    save.reset();
    // Only a move to another claim (or none) refills the form; the draft typed meanwhile is the user's.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editing]);

  const change = (next: Partial<Draft>) => {
    setDraft((d) => ({ ...d, ...next }));
    save.reset();
  };

  // Cancel releases this user's lock; the form goes back to adding once the claims show it released.
  const unlock = useMutation({
    mutationFn: (id: number) => postJson<Claim[]>(byUser(`${claimsPath(sheet)}/${id}/unlock`, user), {}),
    onSuccess: (updated) => queryClient.setQueryData(claimsKey(sheet), updated),
  });
  const cancel = () => {
    save.reset();
    if (editing !== null) unlock.mutate(editing);
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    // The server refuses an amount that is not a number with up to two decimals, a bad date, or a negative amount
    // (a correction) that does not name a SETTLED claim of this person by number, or a
    // reference already used on the sheet (ignoring case).
    send({ id: editing, body: draft });
  };

  return (
    <>
      <td data-testid="person-claimed">{claimed(counted(claims, [person]))}</td>
      <td data-testid="person-due-total">{showDue(personDueCents(claims, rates, person, today, floor, late, cap))}</td>
      <td data-testid="person-excess">{twoDecimals(excess)}</td>
      <td data-testid="person-overdue-count">{lateClaims.count}</td>
      <td data-testid="person-overdue-total">{twoDecimals(lateClaims.cents)}</td>
      <td data-testid="person-aged-0-30">{twoDecimals(aged.upTo30)}</td>
      <td data-testid="person-aged-31-60">{twoDecimals(aged.upTo60)}</td>
      <td data-testid="person-aged-61">{twoDecimals(aged.over60)}</td>
      <td data-testid="person-settled-total">{twoDecimals(split.settled)}</td>
      <td data-testid="person-open-total">{twoDecimals(split.open)}</td>
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
          <input
            data-testid="claim-form-corrects"
            placeholder="corrects #"
            value={draft.corrects}
            onChange={(e) => change({ corrects: e.target.value })}
          />{' '}
          <input
            data-testid="claim-form-ref"
            placeholder="reference"
            value={draft.ref}
            onChange={(e) => change({ ref: e.target.value })}
          />{' '}
          <input
            data-testid="claim-form-tags"
            placeholder="tags, comma separated"
            value={draft.tags}
            onChange={(e) => change({ tags: e.target.value })}
          />{' '}
          <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
            {editing === null ? 'Add claim' : 'Save claim'}
          </button>
          {save.isError && (
            <span data-testid="claim-form-error" role="alert">
              {' '}A claim needs an amount with up to two decimals, and a valid date not after the person's retirement date; a negative amount is a correction and must name, by number, a SETTLED claim of this person; a reference may not be one already used on the sheet; only a DRAFT claim can be edited.
            </span>
          )}
        </form>
        <ul>
          {claims.map((c) => (
            <ClaimRow
              key={c.id}
              sheet={sheet}
              user={user}
              claim={c}
              rates={rates}
              grade={gradeOn(person, c.date)}
              editing={editing === c.id}
              onCancel={cancel}
            />
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
