import { Fragment, useEffect, useState, type ReactNode } from 'react';
import { useIsMutating, useMutation, useQueryClient } from '@tanstack/react-query';
import { getJson, HttpError, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser, useRealToday, useSheetToday } from '../session/session';
import { gradeOn, peopleQueryKey, usePeople, type Person } from '../people/people';
import { chargedOf, useRates } from '../rates/rates';
import { periodOn, usePeriods } from '../periods/periods';
import { policyVersionOn, useSettings } from '../roles/roles';
import { useVoidReasons } from '../voidreasons/voidreasons';
import { templateClaimsPath, useTemplates } from '../templates/templates';
import { claimsPath, claimsQueryKey, dueByOf, isDeferred, isOverdue, isPendingLate, isReviewLate, personClaimsPath, useClaims, useEditingClaim, useSelectedClaims, type Claim } from './claims';

// A person's claims, newest first, with the claim form that adds one — or, while one of their
// claims' editor is open, saves its changes.
function ClaimsHeader() {
  return <th>Claims</th>;
}

function PersonClaims({ person }: { person: Person }) {
  const [sheet] = useCurrentSheet();
  const [user] = useCurrentUser();
  const [editing, setEditing] = useEditingClaim();
  // Bumped each time the editor is submitted: the edited claim's lock error then goes. Counted from the editor's
  // opening, for the claim it opened on, so a submit of the add form (or an earlier editor) does not count.
  const [submits, setSubmits] = useState<{ id: number | null; count: number }>({ id: null, count: 0 });
  // A refused save of the editor closes it (releasing the claim's lock), so the form is back to adding; the add form
  // shows the refusal until it is changed or submitted, or an editor is opened.
  const [refusal, setRefusal] = useState<unknown>(null);
  const { selected, toggle } = useSelectedClaims();
  const claims = useClaims();
  const people = usePeople();
  const rates = useRates();
  const periods = usePeriods();
  const settings = useSettings();
  const today = useSheetToday();
  // The editor shows the claim only once the edit button's lock has answered, so a refused lock is never taken
  // for an open editor.
  const lockingEditor = useIsMutating({ mutationKey: claimLockKey(sheet ?? '', Number(editing) || 0) }) > 0;
  if (!sheet) return null;
  const mine = (claims.data ?? []).filter((c) => c.personId === person.id);
  const claim = editing && !lockingEditor ? mine.find((c) => String(c.id) === editing) : undefined;
  return (
    <td>
      <ul data-testid="claims">
        {mine.map((c) => (
          <li key={c.id} data-testid={`claim-row-${c.id}`}>
            <input
              type="checkbox"
              data-testid="claim-select"
              checked={selected.has(c.id)}
              onChange={(e) => toggle(c.id, e.target.checked)}
            />{' '}
            <span data-testid="claim-number">{c.number ?? ''}</span>{' '}
            <span data-testid="claim-amount">{c.amount}</span>{' '}
            <span data-testid="claim-date">{c.date}</span>{' '}
            <span data-testid="claim-note" style={{ whiteSpace: 'pre' }}>{c.note}</span>{' '}
            <span data-testid="claim-period">{periodOn(periods.data, dueByOf(c))?.name ?? ''}</span>{' '}
            <span data-testid="claim-due">{chargedOf(c, rates.data, gradeOn(person, c.date), settings.data, today)}</span>{' '}
            <span data-testid="claim-policy">{policyVersionOn(settings.data, c.date)}</span>{' '}
            <span data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</span>{' '}
            <span data-testid="claim-opened-by">{c.openedBy ?? ''}</span>{' '}
            <span data-testid="claim-due-by">{dueByOf(c)}</span>{' '}
            <span data-testid="claim-corrects">{c.corrects ?? ''}</span>{' '}
            <span data-testid="claim-overdue">{isOverdue(c, today, gradeOn(person, c.date), settings.data) ? 'yes' : 'no'}</span>{' '}
            {/* Shown while its deferral runs and no statement binds it, whatever its status since; only an OPEN (or
                bound-then-settled) one is held out of the totals. */}
            <span data-testid="claim-deferred">{!c.stated && c.deferredUntil !== null && today !== undefined && today < c.deferredUntil ? 'yes' : 'no'}</span>{' '}
            <span data-testid="claim-deferred-until">{c.deferredUntil ?? ''}</span>{' '}
            <span data-testid="claim-proofs">{c.proofs}</span>{' '}
            (receipts <span data-testid="claim-proofs-receipt">{c.proofsReceipt}</span>, photos{' '}
            <span data-testid="claim-proofs-photo">{c.proofsPhoto}</span>){' '}
            <span data-testid="claim-split-from">{c.splitFrom ?? ''}</span>{' '}
            <span data-testid="claim-late-entry">{c.lateEntry ? 'yes' : 'no'}</span>{' '}
            <span data-testid="claim-waived">{c.waived ? 'yes' : 'no'}</span>{' '}
            <span data-testid="claim-held">{c.held ? 'yes' : 'no'}</span>{' '}
            <span data-testid="claim-reminded">{c.remindedOn ?? ''}</span>{' '}
            <span data-testid="claim-ref">{c.ref ?? ''}</span>{' '}
            <span data-testid="claim-tags">{(c.tags ?? []).join(', ')}</span>{' '}
            <span data-testid="claim-shares">{(c.shares ?? []).map((sh) => `${people.data?.find((p) => p.id === sh.personId)?.name ?? ''} ${sh.share}`).join(', ')}</span>{' '}
            <span data-testid="claim-batch">{c.settleBatch ?? ''}</span>{' '}
            <span data-testid="claim-review-late">{isReviewLate(c, today, settings.data) ? 'yes' : 'no'}</span>{' '}
            <span data-testid="claim-pending-late">{isPendingLate(c, today, settings.data) ? 'yes' : 'no'}</span>{' '}
            <ClaimStatusActions key={`${sheet}:${c.id}`} sheet={sheet} claim={c} onEdit={() => { setRefusal(null); setSubmits({ id: c.id, count: 0 }); setEditing(String(c.id)); }}
              editorSubmits={claim?.id === c.id && submits.id === c.id ? submits.count : 0}>
              {claim?.id === c.id && (
                <button
                  type="button"
                  data-testid="claim-cancel"
                  onClick={() => {
                    setEditing(undefined);
                    // Closing the editor releases the claim's lock, if this user holds it.
                    putJson(`${claimsPath(sheet)}/${c.id}/unlock`, undefined, user).catch(() => undefined);
                  }}
                >
                  Cancel
                </button>
              )}
            </ClaimStatusActions>
            <ol data-testid="claim-lines">
              {(c.lines ?? []).map((line, i) => (
                <li key={i} data-testid="claim-lines-line">{line}</li>
              ))}
            </ol>
            <ol data-testid="claim-comments">
              {(c.comments ?? []).map((line, i) => (
                <li key={i} data-testid="claim-comments-line">{line}</li>
              ))}
            </ol>
            <ol data-testid="claim-log">
              {(c.log ?? []).map((line, i) => (
                <li key={i} data-testid="claim-log-line">{line}</li>
              ))}
            </ol>
            <ClaimTimeline key={`timeline:${sheet}:${c.id}`} sheet={sheet} claimId={c.id} />
          </li>
        ))}
      </ul>
      {/* Keyed by the claim being edited, so opening an editor fills the form with its values. */}
      <ClaimForm key={`${sheet}:${claim?.id ?? ''}`} sheet={sheet} person={person} claim={claim}
        refusal={claim ? null : refusal}
        onRefused={(error, refusedId) => {
          setRefusal(error);
          setEditing(undefined);
          putJson(`${claimsPath(sheet)}/${refusedId}/unlock`, undefined, user).catch(() => undefined);
        }}
        clearRefusal={() => setRefusal(null)}
        onSubmitted={() => setSubmits((s) => claim && s.id === claim.id ? { ...s, count: s.count + 1 } : s)} />
    </td>
  );
}

// A claim's timeline: the audit lines about it, newest first, exactly as the audit file holds them. It reads the
// file each time the button is pressed; while a read runs the old lines are gone, so two reads are told apart.
function ClaimTimeline({ sheet, claimId }: { sheet: string; claimId: number }) {
  const show = useMutation({
    mutationFn: () => getJson<string[]>(`${claimsPath(sheet)}/${claimId}/timeline`),
  });
  return (
    <>
      <button type="button" data-testid="claim-timeline" onClick={() => show.mutate()}>Timeline</button>
      <ol data-testid="claim-timeline-lines">
        {(show.isPending ? [] : show.data ?? []).map((line, i) => (
          <li key={i} data-testid="claim-timeline-line">{line}</li>
        ))}
      </ol>
    </>
  );
}

const ACTIONS = [
  { action: 'open', label: 'Open' },
  { action: 'review', label: 'Review' },
  { action: 'settle', label: 'Settle' },
  { action: 'approve', label: 'Approve' },
  { action: 'reject', label: 'Reject' },
  { action: 'void', label: 'Void' },
  { action: 'resettle', label: 'Re-settle' },
  { action: 'delete', label: 'Delete' },
  { action: 'restore', label: 'Restore' },
] as const;

// A claim's status and the buttons that change it. A change its status does not allow is refused
// by the server and shows the claim's error, as is settling or approving a claim the signed-in user opened, a
// non-steward (other than the person's approver) approving or rejecting a PENDING claim, an open, settle or void by a role the permission matrix does
// not allow, or rejecting one without a reason (typed beside the reject
// button), or a review by anyone but a steward. A claim whose due is above the review threshold in force on its date goes to
// REVIEW when opened; a steward's review button opens it. A claim is numbered when it is settled or approved. A clerk settling a claim whose due is above the clerk limit leaves it PENDING. The error goes as soon as
// the next action on the row is taken. Only a DRAFT can be edited: the edit button of any other claim shows the claim's error instead of opening
// its editor (an open claim is voided and added anew); that of a DRAFT whose due-by is in a closed period opens its
// editor but shows the claim's error, and the editor cannot save. Opening a
// DRAFT's editor locks the claim for other users until it is saved or cancelled, or the sheet's today changes: another
// user's edit button on a locked claim (and a viewer's) shows the claim's error, and their editor cannot save. A DRAFT's
// amount can also be saved in the row (the amount typed beside the edit button), by the claim form's rules: a refusal
// shows the claim's error. A role the permission matrix allows (a steward, by the standing
// rule) moves a DRAFT, OPEN or PENDING
// claim to another person (picked beside the move button); anyone else moving it, or moving a SETTLED or VOID claim,
// shows the claim's error. A steward voids a SETTLED claim within 7 days of its settlement (its frozen due leaves
// the totals); anyone else, or a later day, shows the claim's error. A steward re-settles a SETTLED claim at the figure it would have today as an open claim
// bound by no statement; anyone else, or any other status, shows the claim's error. An OPEN claim is deferred until
// a date after the sheet's today (typed beside the defer button); any other status, a date not after today, or a claim
// whose due-by is in a closed period shows the claim's error. A deferred claim cannot be settled. The proof button raises the claim's
// proofs count by one, of the kind chosen beside it (when none is chosen, a receipt while the sheet has proof kinds, else of no kind); on a SETTLED or VOID claim it shows the claim's error. A claim whose due is above the proof
// threshold in force on its date cannot be settled or approved while it has no proof, nor while it lacks a proof of each of the sheet's proof kinds: that shows the claim's error.
// An OPEN claim splits off an amount (typed beside the split button; more than 0.00 and less than its amount) into a
// new OPEN claim; any other status, a deferred claim, a claim whose due-by is in a closed period or an amount out of range shows the
// claim's error. An OPEN claim is part-settled (the amount typed beside the settle-part button): the amount is split off as
// the split does and the new claim settled at once as the settle does; a refused split or settle (four-eyes and proof
// judged on the original) shows the claim's error and changes nothing. A steward re-dates an OPEN or PENDING claim (the date typed beside the re-date button); anyone
// else, any other status, a claim whose due-by is in a closed period, or a new date after the person's retirement shows
// the claim's error. Re-dating keeps the due-by, so the claim keeps its period. An OPEN or PENDING claim is
// reminded once per sheet's today (its reminded date becomes today); a second press that day, or any other status, shows
// the claim's error. A steward waives an OPEN or PENDING claim's late charge (it bears none from then on, whatever its
// overdue state); anyone else, any other status, or a claim already waived shows the claim's error. An OPEN or PENDING
// claim is held, or released when held (a held claim still counts in every total); any other
// status shows the claim's error. While the sheet's list of void reasons is not empty, a void needs a reason chosen from it (picked
// beside the void button); a void without one shows the claim's error. A claim of any status is commented with the
// text typed beside the comment button (shown on the claim, oldest first, as <user> on <date>: <text>); an empty
// comment shows the claim's error. A DRAFT gains a line (a label, a whole quantity of at least 1 and a unit amount with
// up to two decimals, typed beside the add-line button; shown on the claim as <label> <qty> x <unit>), which makes its
// amount its lines' total; adding a line edits the claim, so a claim that is not a DRAFT, whose due-by is in a closed
// period or that another user has locked, or a line with a field out of rule, shows the claim's error, and an edit of a
// claim with lines that types another amount is refused. A DRAFT is deleted (DELETED: it counts nowhere and cannot be edited, opened or
// voided); any other status, or a claim whose due-by is in a closed period, shows the claim's error. On the sheet's today
// it was deleted, restore makes it a DRAFT again; on a later day, or for a claim that is not DELETED, it shows the claim's error.
function ClaimStatusActions({ sheet, claim, onEdit, editorSubmits, children }: {
  sheet: string; claim: Claim; onEdit: () => void; editorSubmits: number; children: ReactNode;
}) {
  const [user] = useCurrentUser();
  const queryClient = useQueryClient();
  const [reason, setReason] = useState('');
  const voidReasons = useVoidReasons();
  const settings = useSettings();
  const [voidReason, setVoidReason] = useState('');
  // The date a settle is made as of: '' for the sheet's today.
  const [settleOn, setSettleOn] = useState('');
  const change = useMutation({
    mutationFn: ({ action, reason, on }: { action: string; reason?: string; on?: string }) =>
      putJson<Claim>(`${claimsPath(sheet)}/${claim.id}/${action}`
        + (reason !== undefined ? `?reason=${encodeURIComponent(reason)}`
          : on !== undefined ? `?on=${encodeURIComponent(on)}` : ''), undefined, user),
    onSuccess: (_, { action }) => {
      if (action === 'reject') setReason('');
      if (action === 'void') setVoidReason('');
      if (action === 'settle') setSettleOn('');
      return queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) });
    },
  });
  const submitChange = useSubmitOnce(change);
  const people = usePeople();
  const others = (people.data ?? []).filter((p) => p.id !== claim.personId);
  const [moveTo, setMoveTo] = useState('');
  const target = others.find((p) => String(p.id) === moveTo) ?? others[0];
  const move = useMutation({
    mutationFn: (personId: number) =>
      putJson<Claim>(`${claimsPath(sheet)}/${claim.id}/person/${personId}`, undefined, user),
    onSuccess: () => Promise.all([
      queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) }),
      queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet) }),
    ]),
  });
  const submitMove = useSubmitOnce(move);
  const today = useRealToday();
  const [deferTo, setDeferTo] = useState('');
  const defer = useMutation({
    mutationFn: (until: string) =>
      putJson<Claim>(`${claimsPath(sheet)}/${claim.id}/defer?until=${encodeURIComponent(until)}`, undefined, user),
    onSuccess: () => {
      setDeferTo('');
      return queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) });
    },
  });
  const submitDefer = useSubmitOnce(defer);
  const [redateTo, setRedateTo] = useState('');
  const redate = useMutation({
    mutationFn: (date: string) =>
      putJson<Claim>(`${claimsPath(sheet)}/${claim.id}/redate?date=${encodeURIComponent(date)}`, undefined, user),
    onSuccess: () => {
      setRedateTo('');
      return queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) });
    },
  });
  const submitRedate = useSubmitOnce(redate);
  const [dueByTo, setDueByTo] = useState('');
  const dueBy = useMutation({
    mutationFn: (date: string) =>
      putJson<Claim>(`${claimsPath(sheet)}/${claim.id}/due-by?date=${encodeURIComponent(date)}`, undefined, user),
    onSuccess: () => {
      setDueByTo('');
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet) }),
      ]);
    },
  });
  const submitDueBy = useSubmitOnce(dueBy);
  // The kind of the next proof: '' when none is chosen (a receipt while the sheet has proof kinds, else no kind is recorded).
  const [proofKind, setProofKind] = useState('');
  const proof = useMutation({
    mutationFn: (kind: string) =>
      putJson<Claim>(`${claimsPath(sheet)}/${claim.id}/proof${kind ? `?kind=${encodeURIComponent(kind)}` : ''}`, undefined, user),
    onSuccess: () => {
      setProofKind('');
      return queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) });
    },
  });
  const submitProof = useSubmitOnce(proof);
  const remind = useMutation({
    mutationFn: () => putJson<Claim>(`${claimsPath(sheet)}/${claim.id}/remind`, undefined, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) }),
  });
  const submitRemind = useSubmitOnce(remind);
  const waive = useMutation({
    mutationFn: () => putJson<Claim>(`${claimsPath(sheet)}/${claim.id}/waive`, undefined, user),
    onSuccess: () => Promise.all([
      queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) }),
      queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet) }),
    ]),
  });
  const submitWaive = useSubmitOnce(waive);
  const hold = useMutation({
    mutationFn: () => putJson<Claim>(`${claimsPath(sheet)}/${claim.id}/hold`, undefined, user),
    onSuccess: () => Promise.all([
      queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) }),
      queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet) }),
    ]),
  });
  const submitHold = useSubmitOnce(hold);
  const [commentText, setCommentText] = useState('');
  const comment = useMutation({
    mutationFn: (text: string) => postJson<Claim>(`${claimsPath(sheet)}/${claim.id}/comments`, { text }, user),
    onSuccess: () => {
      setCommentText('');
      return queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) });
    },
  });
  const submitComment = useSubmitOnce(comment);
  const [shareWith, setShareWith] = useState('');
  const [sharePct, setSharePct] = useState('');
  const shareTo = (people.data ?? []).find((p) => String(p.id) === shareWith) ?? (people.data ?? [])[0];
  const share = useMutation({
    mutationFn: (s: { personId: string; share: string }) =>
      postJson<Claim>(`${claimsPath(sheet)}/${claim.id}/shares`, s, user),
    onSuccess: () => {
      setSharePct('');
      return queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) });
    },
  });
  const submitShare = useSubmitOnce(share);
  const [assignTo, setAssignTo] = useState('');
  const assign = useMutation({
    mutationFn: (to: string) =>
      putJson<Claim>(`${claimsPath(sheet)}/${claim.id}/review/assign?user=${encodeURIComponent(to)}`, undefined, user),
    onSuccess: () => {
      setAssignTo('');
      return queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) });
    },
  });
  const submitAssign = useSubmitOnce(assign);
  const resetAllButShare = () => {
    change.reset(); move.reset(); defer.reset(); redate.reset(); dueBy.reset(); split.reset(); partSettle.reset(); proof.reset();
    remind.reset(); waive.reset(); hold.reset(); assign.reset(); comment.reset(); setEditRefused(false); lock.reset(); inline.reset();
    line.reset();
  };
  const [lineLabel, setLineLabel] = useState('');
  const [lineQty, setLineQty] = useState('');
  const [lineUnit, setLineUnit] = useState('');
  const line = useMutation({
    mutationFn: (l: { label: string; qty: string; unit: string }) =>
      postJson<Claim>(`${claimsPath(sheet)}/${claim.id}/lines`, l, user),
    onSuccess: () => {
      setLineLabel(''); setLineQty(''); setLineUnit('');
      return queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) });
    },
  });
  const submitLine = useSubmitOnce(line);
  const [splitAmount, setSplitAmount] = useState('');
  const split = useMutation({
    mutationFn: (amount: string) =>
      putJson<Claim>(`${claimsPath(sheet)}/${claim.id}/split?amount=${encodeURIComponent(amount)}`, undefined, user),
    onSuccess: () => {
      setSplitAmount('');
      return queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) });
    },
  });
  const submitSplit = useSubmitOnce(split);
  const [partAmount, setPartAmount] = useState('');
  const partSettle = useMutation({
    mutationFn: (amount: string) =>
      putJson<Claim>(`${claimsPath(sheet)}/${claim.id}/settle/part?amount=${encodeURIComponent(amount)}`, undefined, user),
    onSuccess: () => {
      setPartAmount('');
      return queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) });
    },
  });
  const submitPartSettle = useSubmitOnce(partSettle);
  const [inlineAmount, setInlineAmount] = useState('');
  const inline = useMutation({
    mutationFn: (amount: string) =>
      putJson<Claim>(`${claimsPath(sheet)}/${claim.id}`,
        { amount, date: claim.date, note: claim.note, corrects: claim.corrects ?? '', ref: claim.ref ?? '',
          tags: (claim.tags ?? []).join(', ') }, user),
    onSuccess: () => {
      setInlineAmount('');
      return queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) });
    },
  });
  const submitInline = useSubmitOnce(inline);
  const [editRefused, setEditRefused] = useState(false);
  const [, setEditing] = useEditingClaim();
  const lock = useMutation({
    mutationKey: claimLockKey(sheet, claim.id),
    mutationFn: () => putJson<Claim>(`${claimsPath(sheet)}/${claim.id}/lock`, undefined, user),
    // Another user holds the claim's lock: the editor closes (the claim's error says why), so the form is back to
    // adding rather than left on a claim this user cannot save.
    onError: (error) => {
      if (error instanceof HttpError && error.status === 423) setEditing(undefined);
    },
  });
  const submitLock = useSubmitOnce(lock);
  // Submitting the claim's editor is the next action on it: a refused lock's error goes.
  useEffect(() => {
    if (editorSubmits > 0) lock.reset(); inline.reset(); line.reset(); assign.reset();
  }, [editorSubmits]);
  const action = change.variables?.action;
  const periods = usePeriods();
  const closed = !!periodOn(periods.data, dueByOf(claim))?.closed;
  const capOf = (c: Claim) => periodOn(periods.data, dueByOf(c))?.cap ?? null;
  return (
    <>
      <span data-testid="claim-status">{claim.status}</span>{' '}
      <input
        data-testid="claim-reject-reason"
        placeholder="Reason to reject"
        value={reason}
        onChange={(e) => { setReason(e.target.value); change.reset(); defer.reset(); redate.reset(); dueBy.reset(); split.reset(); partSettle.reset(); proof.reset(); remind.reset(); waive.reset(); hold.reset(); assign.reset(); comment.reset(); share.reset(); setEditRefused(false); lock.reset(); inline.reset(); line.reset(); }}
      />{' '}
      <select
        data-testid="claim-void-reason"
        value={voidReason}
        onChange={(e) => { setVoidReason(e.target.value); change.reset(); defer.reset(); redate.reset(); dueBy.reset(); split.reset(); partSettle.reset(); proof.reset(); remind.reset(); waive.reset(); hold.reset(); assign.reset(); comment.reset(); share.reset(); setEditRefused(false); lock.reset(); inline.reset(); line.reset(); }}
      >
        <option value="">(no void reason)</option>
        {(voidReasons.data ?? []).map((r) => (
          <option key={r.id} value={r.reason}>{r.reason}</option>
        ))}
      </select>{' '}
      {ACTIONS.map(({ action, label }) => (
        <Fragment key={action}>
        {action === 'settle' && (
          <>
            <input
              data-testid="claim-settle-date"
              placeholder="Settle as of YYYY-MM-DD"
              value={settleOn}
              onChange={(e) => { setSettleOn(e.target.value); change.reset(); }}
            />{' '}
          </>
        )}
        <button
          type="button"
          data-testid={`claim-${action}`}
          disabled={change.isPending}
          onClick={() => {
            setEditRefused(false); lock.reset(); inline.reset(); line.reset();
            move.reset();
            defer.reset(); redate.reset(); dueBy.reset(); split.reset(); partSettle.reset();
            proof.reset(); remind.reset(); waive.reset(); hold.reset(); assign.reset(); comment.reset(); share.reset();
            submitChange(action === 'reject' ? { action, reason }
              : action === 'void' && voidReason !== '' ? { action, reason: voidReason }
              : action === 'settle' && settleOn.trim() !== '' ? { action, on: settleOn.trim() } : { action });
          }}
        >
          {label}
        </button>
        </Fragment>
      ))}{' '}
      <select
        data-testid="claim-reassign-to"
        value={target ? String(target.id) : ''}
        onChange={(e) => { setMoveTo(e.target.value); move.reset(); change.reset(); defer.reset(); redate.reset(); dueBy.reset(); split.reset(); partSettle.reset(); proof.reset(); remind.reset(); waive.reset(); hold.reset(); assign.reset(); comment.reset(); share.reset(); setEditRefused(false); lock.reset(); inline.reset(); line.reset(); }}
      >
        {others.map((p) => (
          <option key={p.id} value={String(p.id)}>{p.name}</option>
        ))}
      </select>{' '}
      <button
        type="button"
        data-testid="claim-reassign"
        disabled={move.isPending || !target}
        onClick={() => {
          change.reset();
          defer.reset(); redate.reset(); dueBy.reset(); split.reset(); partSettle.reset();
          proof.reset(); remind.reset(); waive.reset(); hold.reset(); assign.reset(); comment.reset(); share.reset();
          setEditRefused(false); lock.reset(); inline.reset(); line.reset();
          if (target) submitMove(target.id);
        }}
      >
        Move
      </button>{' '}
      <input
        data-testid="claim-defer-date"
        placeholder="Defer until YYYY-MM-DD"
        value={deferTo}
        onChange={(e) => { setDeferTo(e.target.value); defer.reset(); redate.reset(); dueBy.reset(); split.reset(); partSettle.reset(); change.reset(); move.reset(); proof.reset(); remind.reset(); waive.reset(); hold.reset(); assign.reset(); comment.reset(); share.reset(); setEditRefused(false); lock.reset(); inline.reset(); line.reset(); }}
      />{' '}
      <button
        type="button"
        data-testid="claim-defer"
        disabled={defer.isPending}
        onClick={() => {
          change.reset();
          move.reset();
          proof.reset(); remind.reset(); waive.reset(); hold.reset(); assign.reset(); comment.reset(); share.reset();
          split.reset(); partSettle.reset();
          setEditRefused(false); lock.reset(); inline.reset(); line.reset();
          submitDefer(deferTo);
        }}
      >
        Defer
      </button>{' '}
      <input
        data-testid="claim-date-new"
        placeholder="Re-date to YYYY-MM-DD"
        value={redateTo}
        onChange={(e) => { setRedateTo(e.target.value); redate.reset(); dueBy.reset(); defer.reset(); split.reset(); partSettle.reset(); change.reset(); move.reset(); proof.reset(); remind.reset(); waive.reset(); hold.reset(); assign.reset(); comment.reset(); share.reset(); setEditRefused(false); lock.reset(); inline.reset(); line.reset(); }}
      />{' '}
      <button
        type="button"
        data-testid="claim-redate"
        disabled={redate.isPending}
        onClick={() => {
          change.reset();
          move.reset();
          defer.reset();
          proof.reset(); remind.reset(); waive.reset(); hold.reset(); assign.reset(); comment.reset(); share.reset();
          split.reset(); partSettle.reset();
          setEditRefused(false); lock.reset(); inline.reset(); line.reset();
          submitRedate(redateTo);
        }}
      >
        Re-date
      </button>{' '}
      <input
        data-testid="claim-due-by-new"
        placeholder="Due by YYYY-MM-DD"
        value={dueByTo}
        onChange={(e) => { setDueByTo(e.target.value); dueBy.reset(); redate.reset(); defer.reset(); split.reset(); partSettle.reset(); change.reset(); move.reset(); proof.reset(); remind.reset(); waive.reset(); hold.reset(); assign.reset(); comment.reset(); share.reset(); setEditRefused(false); lock.reset(); inline.reset(); line.reset(); }}
      />{' '}
      <button
        type="button"
        data-testid="claim-set-due-by"
        disabled={dueBy.isPending}
        onClick={() => {
          change.reset(); move.reset(); defer.reset(); redate.reset();
          proof.reset(); remind.reset(); waive.reset(); hold.reset(); assign.reset(); comment.reset(); share.reset();
          split.reset(); partSettle.reset();
          setEditRefused(false); lock.reset(); inline.reset(); line.reset();
          submitDueBy(dueByTo);
        }}
      >
        Set due-by
      </button>{' '}
      <select
        data-testid="claim-proof-kind"
        value={proofKind}
        onChange={(e) => { setProofKind(e.target.value); proof.reset(); }}
      >
        <option value="">Kind (receipt)</option>
        <option value="receipt">receipt</option>
        <option value="photo">photo</option>
      </select>{' '}
      <button
        type="button"
        data-testid="claim-proof"
        disabled={proof.isPending}
        onClick={() => {
          change.reset();
          move.reset();
          defer.reset(); redate.reset(); dueBy.reset(); split.reset(); partSettle.reset();
          setEditRefused(false); lock.reset(); inline.reset(); line.reset();
          submitProof(proofKind);
        }}
      >
        Add proof
      </button>{' '}
      <button
        type="button"
        data-testid="claim-remind"
        disabled={remind.isPending}
        onClick={() => {
          change.reset();
          move.reset();
          defer.reset(); redate.reset(); dueBy.reset(); split.reset(); partSettle.reset();
          proof.reset(); comment.reset(); share.reset(); waive.reset(); hold.reset(); assign.reset();
          setEditRefused(false); lock.reset(); inline.reset(); line.reset();
          submitRemind(undefined);
        }}
      >
        Remind
      </button>{' '}
      <button
        type="button"
        data-testid="claim-waive"
        disabled={waive.isPending}
        onClick={() => {
          change.reset();
          move.reset();
          defer.reset(); redate.reset(); dueBy.reset(); split.reset(); partSettle.reset();
          proof.reset(); remind.reset(); comment.reset(); share.reset(); hold.reset(); assign.reset();
          setEditRefused(false); lock.reset(); inline.reset(); line.reset();
          submitWaive(undefined);
        }}
      >
        Waive late charge
      </button>{' '}
      <button
        type="button"
        data-testid="claim-hold"
        disabled={hold.isPending}
        onClick={() => {
          change.reset();
          move.reset();
          defer.reset(); redate.reset(); dueBy.reset(); split.reset(); partSettle.reset();
          proof.reset(); remind.reset(); waive.reset(); comment.reset(); share.reset(); assign.reset();
          setEditRefused(false); lock.reset(); inline.reset(); line.reset();
          submitHold(undefined);
        }}
      >
        {claim.held ? 'Release' : 'Hold'}
      </button>{' '}
      <span data-testid="claim-review-assigned">{claim.reviewAssigned ?? ''}</span>{' '}
      <input
        data-testid="claim-review-user"
        placeholder="Assign review to"
        value={assignTo}
        onChange={(e) => { setAssignTo(e.target.value); assign.reset(); resetAllButShare(); share.reset(); }}
      />{' '}
      <button
        type="button"
        data-testid="claim-review-assign"
        disabled={assign.isPending}
        onClick={() => {
          resetAllButShare(); share.reset();
          submitAssign(assignTo);
        }}
      >
        Assign review
      </button>{' '}
      <input
        data-testid="claim-comment-text"
        placeholder="Comment"
        value={commentText}
        onChange={(e) => { setCommentText(e.target.value); comment.reset(); share.reset(); change.reset(); move.reset(); defer.reset(); redate.reset(); dueBy.reset(); split.reset(); partSettle.reset(); proof.reset(); remind.reset(); waive.reset(); hold.reset(); assign.reset(); setEditRefused(false); lock.reset(); inline.reset(); line.reset(); }}
      />{' '}
      <button
        type="button"
        data-testid="claim-comment-add"
        disabled={comment.isPending}
        onClick={() => {
          change.reset();
          move.reset();
          defer.reset(); redate.reset(); dueBy.reset(); split.reset(); partSettle.reset();
          proof.reset(); remind.reset(); waive.reset(); hold.reset(); assign.reset(); share.reset();
          setEditRefused(false); lock.reset(); inline.reset(); line.reset();
          submitComment(commentText);
        }}
      >
        Comment
      </button>{' '}
      <select
        data-testid="claim-share-select"
        value={shareTo ? String(shareTo.id) : ''}
        onChange={(e) => { setShareWith(e.target.value); share.reset(); resetAllButShare(); }}
      >
        {(people.data ?? []).map((p) => (
          <option key={p.id} value={String(p.id)}>{p.name}</option>
        ))}
      </select>{' '}
      <input
        data-testid="claim-share-pct"
        placeholder="Share %"
        inputMode="decimal"
        value={sharePct}
        onChange={(e) => { setSharePct(e.target.value); share.reset(); resetAllButShare(); }}
      />{' '}
      <button
        type="button"
        data-testid="claim-share-add"
        disabled={share.isPending}
        onClick={() => {
          resetAllButShare();
          submitShare({ personId: shareTo ? String(shareTo.id) : '', share: sharePct });
        }}
      >
        Share
      </button>{' '}
      <input
        data-testid="claim-line-label"
        placeholder="Line label"
        value={lineLabel}
        onChange={(e) => { setLineLabel(e.target.value); line.reset(); change.reset(); move.reset(); defer.reset(); redate.reset(); dueBy.reset(); split.reset(); partSettle.reset(); proof.reset(); remind.reset(); waive.reset(); hold.reset(); assign.reset(); comment.reset(); share.reset(); setEditRefused(false); lock.reset(); inline.reset(); }}
      />{' '}
      <input
        data-testid="claim-line-qty"
        placeholder="Qty"
        inputMode="numeric"
        value={lineQty}
        onChange={(e) => { setLineQty(e.target.value); line.reset(); change.reset(); move.reset(); defer.reset(); redate.reset(); dueBy.reset(); split.reset(); partSettle.reset(); proof.reset(); remind.reset(); waive.reset(); hold.reset(); assign.reset(); comment.reset(); share.reset(); setEditRefused(false); lock.reset(); inline.reset(); }}
      />{' '}
      <input
        data-testid="claim-line-unit"
        placeholder="Unit amount"
        inputMode="decimal"
        value={lineUnit}
        onChange={(e) => { setLineUnit(e.target.value); line.reset(); change.reset(); move.reset(); defer.reset(); redate.reset(); dueBy.reset(); split.reset(); partSettle.reset(); proof.reset(); remind.reset(); waive.reset(); hold.reset(); assign.reset(); comment.reset(); share.reset(); setEditRefused(false); lock.reset(); inline.reset(); }}
      />{' '}
      <button
        type="button"
        data-testid="claim-line-add"
        disabled={line.isPending}
        onClick={() => {
          change.reset();
          move.reset();
          defer.reset(); redate.reset(); dueBy.reset(); split.reset(); partSettle.reset();
          proof.reset(); remind.reset(); waive.reset(); hold.reset(); assign.reset(); comment.reset(); share.reset();
          setEditRefused(false); lock.reset(); inline.reset();
          submitLine({ label: lineLabel, qty: lineQty, unit: lineUnit });
        }}
      >
        Add line
      </button>{' '}
      <input
        data-testid="claim-split-amount"
        placeholder="Amount to split off"
        inputMode="decimal"
        value={splitAmount}
        onChange={(e) => { setSplitAmount(e.target.value); split.reset(); partSettle.reset(); change.reset(); move.reset(); defer.reset(); redate.reset(); dueBy.reset(); proof.reset(); remind.reset(); waive.reset(); hold.reset(); assign.reset(); comment.reset(); share.reset(); setEditRefused(false); lock.reset(); inline.reset(); line.reset(); }}
      />{' '}
      <button
        type="button"
        data-testid="claim-split"
        disabled={split.isPending}
        onClick={() => {
          change.reset();
          move.reset();
          defer.reset(); redate.reset(); dueBy.reset();
          proof.reset(); remind.reset(); waive.reset(); hold.reset(); assign.reset(); comment.reset(); share.reset();
          setEditRefused(false); lock.reset(); inline.reset(); line.reset();
          partSettle.reset();
          submitSplit(splitAmount);
        }}
      >
        Split
      </button>{' '}
      <input
        data-testid="claim-settle-part-amount"
        placeholder="Amount to settle"
        inputMode="decimal"
        value={partAmount}
        onChange={(e) => { setPartAmount(e.target.value); partSettle.reset(); split.reset(); change.reset(); move.reset(); defer.reset(); redate.reset(); dueBy.reset(); proof.reset(); remind.reset(); waive.reset(); hold.reset(); assign.reset(); comment.reset(); share.reset(); setEditRefused(false); lock.reset(); inline.reset(); line.reset(); }}
      />{' '}
      <button
        type="button"
        data-testid="claim-settle-part"
        disabled={partSettle.isPending}
        onClick={() => {
          change.reset();
          move.reset();
          defer.reset(); redate.reset(); dueBy.reset(); split.reset();
          proof.reset(); remind.reset(); waive.reset(); hold.reset(); assign.reset(); comment.reset(); share.reset();
          setEditRefused(false); lock.reset(); inline.reset(); line.reset();
          submitPartSettle(partAmount);
        }}
      >
        Settle part
      </button>{' '}
      <span onClickCapture={() => { change.reset(); move.reset(); defer.reset(); redate.reset(); dueBy.reset(); split.reset(); partSettle.reset(); proof.reset(); remind.reset(); waive.reset(); hold.reset(); assign.reset(); comment.reset(); share.reset(); setEditRefused(false); lock.reset(); inline.reset(); line.reset(); }}>
        <input
          data-testid="claim-inline-amount"
          placeholder="Amount"
          inputMode="decimal"
          value={inlineAmount}
          onChange={(e) => { setInlineAmount(e.target.value); inline.reset(); }}
        />{' '}
        <button
          type="button"
          data-testid="claim-inline-save"
          disabled={inline.isPending}
          onClick={() => submitInline(inlineAmount)}
        >
          Save amount
        </button>{' '}
        <button
          type="button"
          data-testid="claim-edit"
          onClick={() => {
            if (claim.status !== 'DRAFT') {
              setEditRefused(true);
            } else {
              // A DRAFT whose due-by is in a closed period opens its editor, but the lock refuses it (the
              // claim's error) and the editor cannot save.
              onEdit();
              submitLock(undefined);
            }
          }}
        >
          Edit
        </button>
        {children}
      </span>
      {(assign.isError || change.isError || move.isError || defer.isError || redate.isError || dueBy.isError || proof.isError || remind.isError || waive.isError || hold.isError || comment.isError || share.isError || line.isError || split.isError || partSettle.isError || editRefused || lock.isError || inline.isError) && (
        <span data-testid="claim-error">
          {assign.isError
            ? assign.error instanceof HttpError && assign.error.status === 403
              ? 'A viewer cannot assign a review, and no review on an archived sheet can be assigned.'
              : claim.status !== 'REVIEW'
              ? `Only a claim in review can have its review assigned; this one is ${claim.status}.`
              : (assign.variables ?? '').trim() === ''
              ? 'Name the user to assign the review to.'
              : 'Only a steward can assign a review.'
            : share.isError
            ? share.error instanceof HttpError && share.error.status === 403
              ? 'A viewer cannot share a claim, and no claim on an archived sheet can be shared.'
              : claim.status === 'SETTLED' || claim.status === 'VOID' || claim.status === 'DELETED'
              ? `A ${claim.status} claim cannot be shared.`
              : share.variables?.personId === String(claim.personId)
              ? 'A claim cannot be shared with its own person.'
              : (claim.shares ?? []).some((sh) => String(sh.personId) === share.variables?.personId)
              ? 'That person already holds a share of this claim.'
              : 'A share must have up to two decimals, more than 0.00, and the claim\'s shares cannot total more than 100.00.'
            : line.isError
            ? line.error instanceof HttpError && line.error.status === 423
              ? 'Another user is editing this claim; no line can be added until they save or cancel.'
              : line.error instanceof HttpError && line.error.status === 403
              ? 'A viewer cannot edit a claim, and no claim on an archived sheet can be edited.'
              : claim.status !== 'DRAFT'
              ? `Only a draft claim can have lines; this one is ${claim.status}.`
              : closed
              ? 'This claim is due by a date in a closed period, so no line can be added.'
              : 'A line needs a label, a whole quantity of at least 1 and a unit amount with up to two decimals.'
            : inline.isError
            ? inline.error instanceof HttpError && inline.error.status === 423
              ? 'Another user is editing this claim; its amount cannot change until they save or cancel.'
              : inline.error instanceof HttpError && inline.error.status === 403
              ? 'A viewer cannot edit a claim.'
              : claim.status !== 'DRAFT'
              ? `Only a draft claim can be edited; void this ${claim.status} claim and add a new one.`
              : closed
              ? 'This claim is due by a date in a closed period, so it cannot be edited.'
              : (claim.lines ?? []).length > 0
              ? `This claim has lines: its amount is their total (${claim.amount}).`
              : 'The amount must be a number with up to two decimals; a negative amount is a correction and must name a settled claim of this person.'
            : lock.isError
            ? lock.error instanceof HttpError && lock.error.status === 423
              ? 'Another user is editing this claim; it cannot be edited until they save or cancel.'
              : lock.error instanceof HttpError && lock.error.status === 403
              ? 'A viewer cannot edit a claim.'
              : closed
              ? 'This claim is due by a date in a closed period, so it cannot be edited.'
              : `Only a draft claim can be edited; void this ${claim.status} claim and add a new one.`
            : partSettle.isError
            ? partSettle.error instanceof HttpError && partSettle.error.status === 403
              ? 'A viewer cannot settle a claim, and no claim on an archived sheet can be settled.'
              : closed
              ? 'This claim is due by a date in a closed period, so no part of it can be settled.'
              : claim.status !== 'OPEN' || isDeferred(claim, today)
              ? `Only an open claim that is not deferred can be part-settled; this one is ${isDeferred(claim, today) ? 'deferred' : claim.status}.`
              : `No part of this claim can be settled: the amount must be more than 0.00 and less than its ${claim.amount}, and you must be able to settle the claim itself (not its opener or last editor, with the proof its due needs).`
            : split.isError
            ? closed
              ? 'This claim is due by a date in a closed period, so it cannot be split.'
              : claim.status !== 'OPEN' || isDeferred(claim, today)
              ? `Only an open claim that is not deferred can be split; this one is ${isDeferred(claim, today) ? 'deferred' : claim.status}.`
              : `The amount to split off must be more than 0.00 and less than the claim's ${claim.amount}.`
            : comment.isError
            ? comment.error instanceof HttpError && comment.error.status === 403
              ? 'A viewer cannot comment a claim, and no claim on an archived sheet can be commented.'
              : (comment.variables ?? '').trim() === ''
              ? 'A comment cannot be empty.'
              : 'This claim cannot be commented while the sheet is shown as of a date.'
            : remind.isError
            ? remind.error instanceof HttpError && remind.error.status === 403
              ? 'A viewer cannot remind a claim.'
              : claim.status !== 'OPEN' && claim.status !== 'PENDING'
              ? `Only an open or pending claim can be reminded; this one is ${claim.status}.`
              : `This claim was already reminded today (${today ?? ''}).`
            : waive.isError
            ? waive.error instanceof HttpError && waive.error.status === 403
              ? 'A viewer cannot waive a claim\'s late charge.'
              : claim.status !== 'OPEN' && claim.status !== 'PENDING'
              ? `Only an open or pending claim's late charge can be waived; this one is ${claim.status}.`
              : claim.waived
              ? 'This claim\'s late charge is already waived.'
              : 'Only a steward can waive a claim\'s late charge.'
            : hold.isError
            ? hold.error instanceof HttpError && hold.error.status === 403
              ? 'A viewer cannot hold or release a claim.'
              : `Only an open or pending claim can be held or released; this one is ${claim.status}.`
            : proof.isError
            ? `A ${claim.status} claim cannot gain a proof.`
            : dueBy.isError
            ? claim.status !== 'DRAFT' && claim.status !== 'OPEN' && claim.status !== 'PENDING'
              ? `Only a draft, open or pending claim can have its due-by set; this one is ${claim.status}.`
              : 'Only a steward can set a claim\'s due-by, to a date, while neither its old nor its new due-by is in a closed period.'
            : redate.isError
            ? claim.status !== 'OPEN' && claim.status !== 'PENDING'
              ? `Only an open or pending claim can be re-dated; this one is ${claim.status}.`
              : closed
              ? 'This claim is due by a date in a closed period, so it cannot be re-dated.'
              : 'Only a steward can re-date a claim, while its due-by is in no closed period, to a date not after the person\'s retirement.'
            : defer.isError
            ? closed
              ? 'This claim is due by a date in a closed period, so it cannot be deferred.'
              : claim.status !== 'OPEN'
              ? `Only an open claim can be deferred; this one is ${claim.status}.`
              : `A claim can only be deferred to a date after the sheet's today (${today ?? ''}).`
            : move.isError
            ? move.error instanceof HttpError && move.error.status === 403
              ? 'Your role is not allowed to move a claim to another person.'
              : `A ${claim.status} claim cannot move to another person.`
            : editRefused && closed
            ? 'This claim is due by a date in a closed period, so it cannot be edited.'
            : editRefused
            ? `Only a draft claim can be edited; void this ${claim.status} claim and add a new one.`
            : closed
            ? 'This claim is due by a date in a closed period, so its status cannot change.'
            : action === 'review' && claim.status === 'REVIEW' && change.error instanceof HttpError && change.error.status === 403
            ? 'Only a steward can review a claim.'
            : (action === 'approve' || action === 'reject') && change.error instanceof HttpError && change.error.status === 403
            ? 'Only a steward or the person\'s approver can approve or reject a pending claim.'
            : action === 'void' && claim.status === 'SETTLED' && change.error instanceof HttpError && change.error.status === 403
            ? 'Only a steward can void a settled claim.'
            : (action === 'open' || action === 'settle' || action === 'void') && change.error instanceof HttpError && change.error.status === 403
            ? `Your role is not allowed to ${action} a claim.`
            : action === 'void' && (voidReasons.data ?? []).length > 0 && !change.variables?.reason
            ? 'Choose a void reason from the sheet\'s list to void this claim.'
            : action === 'void' && claim.status === 'SETTLED'
            ? 'A settled claim can be voided only within 7 days of its settlement.'
            : action === 'open' && claim.status === 'DRAFT' && capOf(claim) !== null
            ? `This claim is due in period ${periodOn(periods.data, dueByOf(claim))?.name}, which caps each person at ${capOf(claim)} open or pending claims; this person already has that many there.`
            : action === 'delete'
            ? `Only a draft claim can be deleted; this one is ${claim.status}.`
            : action === 'restore' && claim.status === 'DELETED'
            ? 'A deleted claim can be restored only on the day it was deleted.'
            : action === 'restore'
            ? `Only a deleted claim can be restored; this one is ${claim.status}.`
            : action === 'resettle' && change.error instanceof HttpError && change.error.status === 403
            ? 'Only a steward can re-settle a claim.'
            : action === 'resettle'
            ? `Only a settled claim can be re-settled; this one is ${claim.status}.`
            : (action === 'settle' || action === 'approve') && change.error instanceof HttpError && change.error.status === 422
            ? (settings.data?.proofKinds ?? '') !== ''
              ? `This claim's due is above the proof threshold, so it needs a proof of each kind (${settings.data?.proofKinds}) before it can be settled or approved.`
              : 'This claim\'s due is above the proof threshold, so it needs a proof before it can be settled or approved.'
            : action === 'settle' && change.variables?.on !== undefined && today !== undefined && change.variables.on > today
            ? `A claim can be settled only as of a date on or before the sheet's today (${today}).`
            : action === 'settle' && change.variables?.on !== undefined && !/^\d{4}-\d{2}-\d{2}$/.test(change.variables.on)
            ? 'Settle as of a date written YYYY-MM-DD.'
            : action === 'settle' && isDeferred(claim, today)
            ? `This claim is deferred until ${claim.deferredUntil}, so it cannot be settled before then.`
            : action === 'settle' && claim.openedBy === user
            ? 'You opened this claim, so someone else must settle it.'
            : action === 'approve' && claim.openedBy === user && claim.status === 'PENDING'
            ? 'You opened this claim, so someone else must approve it.'
            : action === 'settle' && claim.editedBy === user
            ? 'You last edited this claim, so someone else must settle it.'
            : action === 'approve' && claim.editedBy === user && claim.status === 'PENDING'
            ? 'You last edited this claim, so someone else must approve it.'
            : action === 'settle' && change.variables?.on !== undefined && claim.status === 'OPEN' && change.error instanceof HttpError && change.error.status === 409
            ? `This claim had not been added by ${change.variables.on}, so it cannot be settled as of then.`
            : action === 'reject' && claim.status === 'PENDING' && reason.trim() === ''
            ? 'Give a reason to reject this claim.'
            : `That status change is not allowed for a ${claim.status} claim.`}
        </span>
      )}
    </>
  );
}

function ClaimForm({ sheet, person, claim, refusal, onRefused, clearRefusal, onSubmitted }: {
  sheet: string; person: Person; claim?: Claim; refusal: unknown;
  onRefused: (error: unknown, claimId: number) => void; clearRefusal: () => void; onSubmitted: () => void;
}) {
  const [user] = useCurrentUser();
  const [, setEditing] = useEditingClaim();
  const today = useRealToday();
  const [amount, setAmount] = useState(claim?.amount ?? '');
  const [date, setDate] = useState(claim?.date ?? '');
  const [note, setNote] = useState(claim?.note ?? '');
  const [corrects, setCorrects] = useState(claim?.corrects == null ? '' : String(claim.corrects));
  const [ref, setRef] = useState(claim?.ref ?? '');
  const [tags, setTags] = useState((claim?.tags ?? []).join(', '));
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (body: { amount: string; date: string; note: string; corrects: string; ref: string; tags: string }) =>
      claim
        ? putJson<Claim>(`${claimsPath(sheet)}/${claim.id}`, body, user)
        : postJson<Claim>(personClaimsPath(sheet, person.id), body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) });
      if (claim) {
        setEditing(undefined);
      } else {
        setAmount('');
        setDate('');
        setNote('');
        setCorrects('');
        setRef('');
        setTags('');
      }
    },
    onError: (error) => {
      if (claim) onRefused(error, claim.id);
    },
  });
  const submit = useSubmitOnce(save);
  // A new claim can instead be made from a template (picked beside the form): its amount and note as they are now,
  // and no date, by every rule of the form; a refusal shows the form's error.
  const templates = useTemplates();
  const [template, setTemplate] = useState('');
  const fromTemplate = useMutation({
    mutationFn: (body: { template: string }) => postJson<Claim>(templateClaimsPath(sheet, person.id), body, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) }),
  });
  const submitTemplate = useSubmitOnce(fromTemplate);
  // The editor cannot save until the edit button's lock has answered (taken, or refused).
  const locking = useIsMutating({ mutationKey: claimLockKey(sheet, claim?.id ?? 0) }) > 0 && !!claim;

  return (
    <form
      data-testid="claim-form"
      onSubmit={(e) => {
        e.preventDefault();
        onSubmitted();
        fromTemplate.reset();
        clearRefusal();
        submit({ amount, date, note, corrects, ref, tags });
      }}
    >
      <input
        data-testid="claim-form-amount"
        placeholder="Amount"
        inputMode="decimal"
        value={amount}
        onChange={(e) => { setAmount(e.target.value); save.reset(); fromTemplate.reset(); clearRefusal(); }}
      />
      <input
        data-testid="claim-form-date"
        placeholder={today ?? 'YYYY-MM-DD'}
        value={date}
        onChange={(e) => { setDate(e.target.value); save.reset(); fromTemplate.reset(); clearRefusal(); }}
      />
      <input
        data-testid="claim-form-note"
        placeholder="Note"
        value={note}
        onChange={(e) => { setNote(e.target.value); save.reset(); fromTemplate.reset(); clearRefusal(); }}
      />
      <input
        data-testid="claim-form-corrects"
        placeholder="Corrects claim no."
        inputMode="numeric"
        value={corrects}
        onChange={(e) => { setCorrects(e.target.value); save.reset(); fromTemplate.reset(); clearRefusal(); }}
      />
      <input
        data-testid="claim-form-ref"
        placeholder="Reference"
        value={ref}
        onChange={(e) => { setRef(e.target.value); save.reset(); fromTemplate.reset(); clearRefusal(); }}
      />
      <input
        data-testid="claim-form-tags"
        placeholder="Tags (comma separated)"
        value={tags}
        onChange={(e) => { setTags(e.target.value); save.reset(); fromTemplate.reset(); clearRefusal(); }}
      />
      <button type="submit" data-testid="claim-form-submit" disabled={save.isPending || locking}>
        {claim ? 'Save claim' : 'Add claim'}
      </button>
      {!claim && (
        <>
          {' '}
          <select
            data-testid="claim-template-select"
            value={template}
            onChange={(e) => { setTemplate(e.target.value); save.reset(); fromTemplate.reset(); clearRefusal(); }}
          >
            <option value="">Template…</option>
            {(templates.data ?? []).map((t) => (
              <option key={t.id} value={t.name}>{t.name}</option>
            ))}
          </select>
          <button
            type="button"
            data-testid="claim-template-add"
            disabled={fromTemplate.isPending}
            onClick={() => { save.reset(); clearRefusal(); submitTemplate({ template }); }}
          >
            Add from template
          </button>
        </>
      )}
      {(save.isError || fromTemplate.isError || refusal !== null) && (
        <span data-testid="claim-form-error">
          {(save.error ?? refusal) instanceof HttpError && (save.error ?? refusal as HttpError).status === 423 && 'Another user is editing this claim. '}
          The amount must be a number with up to two decimals, and the date a calendar date whose due-by (it plus the
          terms) is in no closed period and not after the person's retirement date; a negative amount is a correction and must name, by
          number, a settled claim of this person (only a correction may) and not exceed what remains of it (its amount less the
          corrections already made against it that are not void or deleted); a reference must not already be used on this sheet
          (ignoring case); only a draft claim can be edited, and one with lines keeps their total as its amount; a claim from a template needs a template chosen.
        </span>
      )}
    </form>
  );
}

/** The key of a claim's lock mutation (its edit button), so its editor can wait for it. */
const claimLockKey = (sheet: string, claimId: number) => ['sheet', sheet, 'claims', claimId, 'lock'] as const;

export const contributions = [
  PeopleColumns.fill({ order: 20, Component: ClaimsHeader }),
  PersonCells.fill({ order: 20, Component: PersonClaims }),
];
