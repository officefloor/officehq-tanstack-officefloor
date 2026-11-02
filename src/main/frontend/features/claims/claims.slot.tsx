import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { useShown } from '../columns/columns';
import { gradeOn, peoplePath, usePeople } from '../people/people';
import type { Grade, Person } from '../people/people';
import { useSheetToday } from '../sheet/sheet';
import { claimRateOf, showDue, twoDecimals, useRates } from '../rates/rates';
import type { Rate } from '../rates/rates';
import { periodOf, usePeriods } from '../periods/periods';
import { useCapCents, useFloorCents, useReviewSla } from '../settings/settings';
import {
  claimDueCents,
  claimed,
  claimsKey,
  claimsPath,
  claimedCounted,
  counted,
  overdue,
  reviewLate,
  overdueCents,
  agedCents,
  splitDueCents,
  scoreOf,
  personDueCents,
  uncappedPersonDueCents,
  useClaims,
  useLate,
} from './claims';
import type { Claim, ClaimStatusChange } from './claims';
import { selectClaim, useSelectedClaims } from './selection';
import { useVoidReasons, voidReasonsKey } from '../voidreasons/voidreasons';
import { useTemplates } from '../templates/templates';
import { Fig } from '../locale/Fig';

type Draft = { amount: string; date: string; note: string; corrects: string; ref: string; tags: string };
const EMPTY: Draft = { amount: '', date: '', note: '', corrects: '', ref: '', tags: '' };

function ClaimedHeader({ sheet, user }: { sheet: string; user: string }) {
  const shown = useShown(sheet, user);
  return (
    <>
      {shown('claimed') && <th>Claimed</th>}
      {shown('due-total') && <th>Due total</th>}
      {shown('excess') && <th>Excess</th>}
      {shown('overdue-count') && <th>Overdue</th>}
      {shown('overdue-total') && <th>Overdue total</th>}
      <th>Due 0-30 days</th>
      <th>Due 31-60 days</th>
      <th>Due 61+ days</th>
      <th>Settled total</th>
      <th>Open total</th>
      <th>Score</th>
      <th>Claims</th>
    </>
  );
}

const CHANGES: { change: ClaimStatusChange; label: string }[] = [
  { change: 'open', label: 'Open' },
  { change: 'settle', label: 'Settle' },
  { change: 'void', label: 'Void' },
  { change: 'approve', label: 'Approve' },
  // A DRAFT outside a closed period is deleted (it then counts nowhere); restored to a DRAFT only on the sheet's today it was deleted.
  { change: 'delete', label: 'Delete' },
  { change: 'restore', label: 'Restore' },
  // A steward's review opens a REVIEW claim (it is numbered only when settled).
  { change: 'review', label: 'Review' },
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
  const reviewSla = useReviewSla(sheet);
  // While the sheet has void reasons, a void needs one, chosen beside the Void button.
  const { data: voidReasons = [] } = useVoidReasons(sheet);
  const [voidReason, setVoidReason] = useState('');
  // A settle is as of the date typed beside the Settle button (blank for the sheet's today).
  const [settleOn, setSettleOn] = useState('');
  const status = useMutation({
    mutationFn: (change: ClaimStatusChange) =>
      postJson<Claim[]>(
        byUser(`${claimsPath(sheet)}/${claim.id}/${change}`, user),
        change === 'reject'
          ? { reason }
          : change === 'void'
            ? { reason: voidReason }
            : change === 'settle'
              ? { on: settleOn }
              : {},
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
  // A steward waives an OPEN or PENDING claim's late charge, once: from then on it bears none.
  const waiveMutation = useMutation({
    mutationFn: () => postJson<Claim[]>(byUser(`${claimsPath(sheet)}/${claim.id}/waive`, user), {}),
    onSuccess: (updated) => queryClient.setQueryData(claimsKey(sheet), updated),
  });
  const waive = useSubmitOnce(waiveMutation);
  // An OPEN or PENDING claim is held, or released when held: while held (and not bound by a statement) it counts in no due figure, but stays in claimed.
  const holdMutation = useMutation({
    mutationFn: () => postJson<Claim[]>(byUser(`${claimsPath(sheet)}/${claim.id}/hold`, user), {}),
    onSuccess: (updated) => queryClient.setQueryData(claimsKey(sheet), updated),
  });
  const holdClaim = useSubmitOnce(holdMutation);
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
  // A proof raises the claim's proofs count by one (not on a SETTLED or VOID claim, nor one in a closed period), of
  // the kind chosen beside the button (receipt or photo; none chosen counts as a receipt).
  const [proofKind, setProofKind] = useState('');
  const proofMutation = useMutation({
    mutationFn: (kind: string) => postJson<Claim[]>(byUser(`${claimsPath(sheet)}/${claim.id}/proof`, user), { kind }),
    onSuccess: (updated) => {
      setProofKind('');
      queryClient.setQueryData(claimsKey(sheet), updated);
    },
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
  // A DRAFT claim takes a line (label, quantity, unit amount) typed beside the Add line button; its amount becomes the
  // lines' total. The server refuses a claim that is not a DRAFT, one due inside a closed period, or one another user's editor holds.
  const [lineLabel, setLineLabel] = useState('');
  const [lineQty, setLineQty] = useState('');
  const [lineUnit, setLineUnit] = useState('');
  const lineMutation = useMutation({
    mutationFn: (line: { label: string; qty: string; unit: string }) =>
      postJson<Claim[]>(byUser(`${claimsPath(sheet)}/${claim.id}/line`, user), line),
    onSuccess: (updated) => {
      setLineLabel('');
      setLineQty('');
      setLineUnit('');
      queryClient.setQueryData(claimsKey(sheet), updated);
    },
  });
  const addLine = useSubmitOnce(lineMutation);
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
  // An OPEN claim part-settles an amount, typed beside the Settle part button: split off and settled at once.
  const [partAmount, setPartAmount] = useState('');
  const partMutation = useMutation({
    mutationFn: (amount: string) =>
      postJson<Claim[]>(byUser(`${claimsPath(sheet)}/${claim.id}/part`, user), { amount }),
    onSuccess: (updated) => {
      setPartAmount('');
      queryClient.setQueryData(claimsKey(sheet), updated);
    },
  });
  const settlePart = useSubmitOnce(partMutation);
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
  // The claim's timeline: the audit lines about it, newest first, read from the file at each press of its button
  // (so a second press shows the file as it stands then; while a press is being read the old lines are gone).
  const [timelinePress, setTimelinePress] = useState(0);
  const timeline = useQuery({
    queryKey: ['claim-timeline', sheet, claim.id, timelinePress],
    queryFn: () => getJson<string[]>(`${claimsPath(sheet)}/${claim.id}/timeline`),
    enabled: timelinePress > 0,
    gcTime: 0,
  });

  return (
    <li data-testid={`claim-row-${claim.id}`}>
      <input
        type="checkbox"
        data-testid="claim-select"
        checked={selected}
        onChange={(e) => selectClaim(sheet, claim.id, e.target.checked)}
      />{' '}
      <span data-testid="claim-amount"><Fig>{claim.amount}</Fig></span>{' '}
      <span data-testid="claim-date">{claim.date}</span>{' '}
      <span data-testid="claim-note">{claim.note}</span>{' '}
      due <span data-testid="claim-due"><Fig>{showDue(claimDueCents(claim, claimRateOf(rates, grade, claim), floor, late))}</Fig></span>{' '}
      due by <span data-testid="claim-due-by">{claim.dueBy}</span>{' '}
      overdue <span data-testid="claim-overdue">{overdue(claim, late) ? 'yes' : 'no'}</span>{' '}
      <span data-testid="claim-status">{claim.status}</span>{' '}
      review late <span data-testid="claim-review-late">{reviewLate(claim, reviewSla, today) ? 'yes' : 'no'}</span>{' '}
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
      waived <span data-testid="claim-waived">{claim.waived ? 'yes' : 'no'}</span>{' '}
      <button
        type="button"
        data-testid="claim-waive"
        disabled={waiveMutation.isPending}
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
          lineMutation.reset();
          splitMutation.reset();
          partMutation.reset();
          inlineMutation.reset();
          waiveMutation.reset();
          holdMutation.reset();
          waive(undefined);
        }}
      >
        Waive late charge
      </button>{' '}
      held <span data-testid="claim-held">{claim.held ? 'yes' : 'no'}</span>{' '}
      <button
        type="button"
        data-testid="claim-hold"
        disabled={holdMutation.isPending}
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
          lineMutation.reset();
          splitMutation.reset();
          partMutation.reset();
          inlineMutation.reset();
          waiveMutation.reset();
          holdMutation.reset();
          holdClaim(undefined);
        }}
      >
        {claim.held ? 'Release' : 'Hold'}
      </button>{' '}
      proofs <span data-testid="claim-proofs">{claim.proofs}</span>{' '}
      (receipts <span data-testid="claim-proofs-receipt">{claim.proofs - claim.photos}</span>,{' '}
      photos <span data-testid="claim-proofs-photo">{claim.photos}</span>){' '}
      <select
        data-testid="claim-proof-kind"
        value={proofKind}
        onChange={(e) => {
          setProofKind(e.target.value);
          proofMutation.reset();
        }}
      >
        <option value="">kind (receipt)</option>
        <option value="receipt">receipt</option>
        <option value="photo">photo</option>
      </select>{' '}
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
          waiveMutation.reset();
          holdMutation.reset();
          remindMutation.reset();
          redateMutation.reset();
          splitMutation.reset();
          partMutation.reset();
          inlineMutation.reset();
          addProof(proofKind);
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
            waiveMutation.reset();
            holdMutation.reset();
          remindMutation.reset();
            redateMutation.reset();
            splitMutation.reset();
            partMutation.reset();
            inlineMutation.reset();
          inlineMutation.reset();
            proofMutation.reset();
            commentMutation.reset();
            lineMutation.reset();
          lineMutation.reset();
            act(change);
          }}
        >
          {label}
        </button>
      ))}{' '}
      <input
        type="date"
        data-testid="claim-settle-date"
        aria-label="settle as of"
        value={settleOn}
        onChange={(e) => {
          setSettleOn(e.target.value);
          status.reset();
        }}
      />{' '}
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
          partMutation.reset();
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
          waiveMutation.reset();
          holdMutation.reset();
          remindMutation.reset();
          redateMutation.reset();
          splitMutation.reset();
          partMutation.reset();
          inlineMutation.reset();
          proofMutation.reset();
          commentMutation.reset();
          lineMutation.reset();
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
          waiveMutation.reset();
          holdMutation.reset();
          remindMutation.reset();
          redateMutation.reset();
          splitMutation.reset();
          partMutation.reset();
          proofMutation.reset();
          commentMutation.reset();
          lineMutation.reset();
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
          waiveMutation.reset();
          holdMutation.reset();
          remindMutation.reset();
          redateMutation.reset();
          splitMutation.reset();
          partMutation.reset();
          inlineMutation.reset();
          proofMutation.reset();
          commentMutation.reset();
          lineMutation.reset();
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
          partMutation.reset();
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
          waiveMutation.reset();
          holdMutation.reset();
          remindMutation.reset();
          redateMutation.reset();
          splitMutation.reset();
          partMutation.reset();
          inlineMutation.reset();
          proofMutation.reset();
          commentMutation.reset();
          lineMutation.reset();
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
          waiveMutation.reset();
          holdMutation.reset();
          remindMutation.reset();
          redateMutation.reset();
          splitMutation.reset();
          partMutation.reset();
          inlineMutation.reset();
          proofMutation.reset();
          commentMutation.reset();
          lineMutation.reset();
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
          waiveMutation.reset();
          holdMutation.reset();
          remindMutation.reset();
          redateMutation.reset();
          splitMutation.reset();
          partMutation.reset();
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
          lineMutation.reset();
          splitMutation.reset();
          partMutation.reset();
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
          waiveMutation.reset();
          holdMutation.reset();
          redateMutation.reset();
          proofMutation.reset();
          commentMutation.reset();
          lineMutation.reset();
          splitMutation.reset();
          partMutation.reset();
          inlineMutation.reset();
          remindMutation.reset();
          remind(undefined);
        }}
      >
        Remind
      </button>{' '}
      <button type="button" data-testid="claim-timeline" onClick={() => setTimelinePress((n) => n + 1)}>
        Timeline
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
          waiveMutation.reset();
          holdMutation.reset();
          remindMutation.reset();
          proofMutation.reset();
          commentMutation.reset();
          lineMutation.reset();
          splitMutation.reset();
          partMutation.reset();
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
          partMutation.reset();
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
          waiveMutation.reset();
          holdMutation.reset();
          remindMutation.reset();
          redateMutation.reset();
          proofMutation.reset();
          commentMutation.reset();
          lineMutation.reset();
          partMutation.reset();
          split(splitAmount);
        }}
      >
        Split
      </button>{' '}
      <input
        data-testid="claim-settle-part-amount"
        placeholder="settle part amount"
        value={partAmount}
        onChange={(e) => {
          setPartAmount(e.target.value);
          partMutation.reset();
          inlineMutation.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="claim-settle-part"
        disabled={partMutation.isPending}
        onClick={() => {
          clearEditError();
          status.reset();
          move.reset();
          resettleMutation.reset();
          deferMutation.reset();
          waiveMutation.reset();
          holdMutation.reset();
          remindMutation.reset();
          redateMutation.reset();
          proofMutation.reset();
          commentMutation.reset();
          lineMutation.reset();
          splitMutation.reset();
          inlineMutation.reset();
          settlePart(partAmount);
        }}
      >
        Settle part
      </button>{' '}
      <input
        data-testid="claim-comment-text"
        placeholder="comment"
        value={commentText}
        onChange={(e) => {
          setCommentText(e.target.value);
          commentMutation.reset();
          lineMutation.reset();
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
          waiveMutation.reset();
          holdMutation.reset();
          remindMutation.reset();
          redateMutation.reset();
          proofMutation.reset();
          splitMutation.reset();
          partMutation.reset();
          inlineMutation.reset();
          commentMutation.reset();
          lineMutation.reset();
          addComment(commentText);
        }}
      >
        Comment
      </button>{' '}
      <input
        data-testid="claim-line-label"
        placeholder="line label"
        value={lineLabel}
        onChange={(e) => {
          setLineLabel(e.target.value);
          lineMutation.reset();
        }}
      />{' '}
      <input
        data-testid="claim-line-qty"
        placeholder="qty"
        value={lineQty}
        onChange={(e) => {
          setLineQty(e.target.value);
          lineMutation.reset();
        }}
      />{' '}
      <input
        data-testid="claim-line-unit"
        placeholder="unit amount"
        value={lineUnit}
        onChange={(e) => {
          setLineUnit(e.target.value);
          lineMutation.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="claim-line-add"
        disabled={lineMutation.isPending}
        onClick={() => {
          clearEditError();
          status.reset();
          move.reset();
          resettleMutation.reset();
          deferMutation.reset();
          waiveMutation.reset();
          holdMutation.reset();
          remindMutation.reset();
          redateMutation.reset();
          proofMutation.reset();
          splitMutation.reset();
          partMutation.reset();
          inlineMutation.reset();
          commentMutation.reset();
          lineMutation.reset();
          addLine({ label: lineLabel, qty: lineQty, unit: lineUnit });
        }}
      >
        Add line
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
      <ul>
        {claim.lines.map((line, i) => (
          <li key={i} data-testid="claim-lines-line">
            {line.label} {line.qty} x {line.unit}
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
          {' '}A {claim.status} claim cannot be changed that way (your role may not take that action; the user who opened it cannot settle or approve it; only a steward approves or rejects a PENDING claim, a reject needs a reason, a void needs a reason from the sheet's list while it has one, a claim whose due is above the proof threshold needs a proof, one of each of the sheet's proof kinds while it has them, and one whose due is above the note threshold needs a note to be opened; only a steward reviews a REVIEW claim, which is not edited, settled nor split; only a DRAFT outside a closed period is deleted, and a DELETED claim is restored only on the day it was deleted; a settle as of a date needs a date on or before the sheet's today on which the claim already existed).
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
      {waiveMutation.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}This claim's late charge cannot be waived (only a steward waives, only an OPEN or PENDING claim, and only once).
        </span>
      )}
      {holdMutation.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}A {claim.status} claim cannot be held or released (only an OPEN or PENDING claim).
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
          {' '}A {claim.status} claim cannot be given a proof (not a SETTLED or VOID claim, nor one due inside a closed period; a proof is a receipt or a photo).
        </span>
      )}
      {commentMutation.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}This comment cannot be added: it may not be empty, and a viewer, an archived sheet or the as-of view changes nothing.
        </span>
      )}
      {lineMutation.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}This line cannot be added: it needs a label, a whole quantity of at least 1 and a unit amount with up to two decimals, and only a DRAFT outside a closed period takes one, not while another user has its editor open.
        </span>
      )}
      {splitMutation.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}A {claim.status} claim cannot be split (only an OPEN claim, not deferred nor due inside a closed period, by a role that may open a claim, and only an amount more than 0.00 and less than the claim's).
        </span>
      )}
      {partMutation.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}A {claim.status} claim cannot be part-settled (the amount must split off as the Split button does, and the part must settle as the Settle button does: not by the user who opened or last edited the claim, and with a proof while the claim's due is above the proof threshold).
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
      {timeline.isError && (
        <span data-testid="claim-timeline-error" role="alert">
          {' '}The timeline could not be read.
        </span>
      )}
      {timeline.isSuccess && !timeline.isFetching && (
        <ol>
          {timeline.data.map((line, i) => (
            <li key={i} data-testid="claim-timeline-line">
              <code>{line}</code>
            </li>
          ))}
        </ol>
      )}
    </li>
  );
}

// A person's claimed total, then their claims (newest first) under one form that adds a claim or,
// once a claim's editor is opened, edits it. A blank date takes the sheet's today on the server.
function PersonClaims({ sheet, user, person }: { sheet: string; user: string; person: Person }) {
  const shown = useShown(sheet, user);
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
  // A DRAFT claim from a template: its amount and note as they are now, dated like a claim with no date; refused,
  // like the form, through claim-form-error.
  const { data: templates = [] } = useTemplates(sheet);
  const [template, setTemplate] = useState('');
  const fromTemplate = useMutation({
    mutationFn: (name: string) =>
      postJson<Claim[]>(byUser(`${peoplePath(sheet)}/${person.id}/claims/template`, user), { template: name }),
    onSuccess: (updated) => queryClient.setQueryData(claimsKey(sheet), updated),
  });
  const sendFromTemplate = useSubmitOnce(fromTemplate);
  const addFromTemplate = () => {
    save.reset();
    sendFromTemplate(template);
  };

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
    fromTemplate.reset();
  };

  // Cancel releases this user's lock; the form goes back to adding once the claims show it released.
  const unlock = useMutation({
    mutationFn: (id: number) => postJson<Claim[]>(byUser(`${claimsPath(sheet)}/${id}/unlock`, user), {}),
    onSuccess: (updated) => queryClient.setQueryData(claimsKey(sheet), updated),
  });
  const cancel = () => {
    save.reset();
    fromTemplate.reset();
    if (editing !== null) unlock.mutate(editing);
  };

  const submit = (e: FormEvent) => {
    e.preventDefault();
    // The server refuses an amount that is not a number with up to two decimals, a bad date, or a negative amount
    // (a correction) that does not name a SETTLED claim of this person by number, or a
    // reference already used on the sheet (ignoring case).
    fromTemplate.reset();
    send({ id: editing, body: draft });
  };

  return (
    <>
      {shown('claimed') && <td data-testid="person-claimed"><Fig>{claimed(claimedCounted(claims, [person]))}</Fig></td>}
      {shown('due-total') && <td data-testid="person-due-total"><Fig>{showDue(personDueCents(claims, rates, person, today, floor, late, cap))}</Fig></td>}
      {shown('excess') && <td data-testid="person-excess"><Fig>{twoDecimals(excess)}</Fig></td>}
      {shown('overdue-count') && <td data-testid="person-overdue-count">{lateClaims.count}</td>}
      {shown('overdue-total') && <td data-testid="person-overdue-total"><Fig>{twoDecimals(lateClaims.cents)}</Fig></td>}
      <td data-testid="person-aged-0-30"><Fig>{twoDecimals(aged.upTo30)}</Fig></td>
      <td data-testid="person-aged-31-60"><Fig>{twoDecimals(aged.upTo60)}</Fig></td>
      <td data-testid="person-aged-61"><Fig>{twoDecimals(aged.over60)}</Fig></td>
      <td data-testid="person-settled-total"><Fig>{twoDecimals(split.settled)}</Fig></td>
      <td data-testid="person-open-total"><Fig>{twoDecimals(split.open)}</Fig></td>
      <td data-testid="person-score">{scoreOf(split.settled, lateClaims.count).toString()}</td>
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
          <select
            data-testid="claim-template-select"
            value={template}
            onChange={(e) => {
              setTemplate(e.target.value);
              fromTemplate.reset();
            }}
          >
            <option value="">template…</option>
            {templates.map((t) => (
              <option key={t.name} value={t.name}>
                {t.name}
              </option>
            ))}
          </select>{' '}
          <button type="button" data-testid="claim-template-add" disabled={fromTemplate.isPending} onClick={addFromTemplate}>
            Add from template
          </button>
          {(save.isError || fromTemplate.isError) && (
            <span data-testid="claim-form-error" role="alert">
              {' '}A claim needs an amount with up to two decimals, and a valid date not after the person's retirement date; a negative amount is a correction and must name, by number, a SETTLED claim of this person; a reference may not be one already used on the sheet; only a DRAFT claim can be edited; a claim from a template needs one of the sheet's templates chosen.
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
