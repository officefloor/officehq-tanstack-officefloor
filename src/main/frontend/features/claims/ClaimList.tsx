import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson, putJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { claimDue, showDue } from '../rates/due';
import { claimsKey, deferredShown, tagList, type Claim } from './useClaims';
import { useVoidReasons } from '../voidreasons/useVoidReasons';
import { periodOf, usePeriods } from '../periods/usePeriods';
import { useFloor, useLatePct, usePolicy, useSettings } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheetToday';
import { dueBy, isOverdue } from './overdue';
import { useGrace } from './useGrace';
import { ClaimSelect } from './BulkOpen';
import { ClaimTimeline } from './ClaimTimeline';
import { isReviewLate } from './reviewLate';
import { peopleKey, type Person } from '../people/usePeople';

// A person's claims, newest first, each with its amount, date, note, status, number, opened-by, period (the one whose dates contain its due-by as fixed at creation, blank for none), stated (yes when the
// person's last statement bound it), corrects (the number a correction corrects, blank for other claims) and due (fixed when it was settled, else bound by the last statement; otherwise its amount at the person's grade's rate in force on the claim's date, n/a when none applies; an overdue claim gains, for each full 30 days overdue, the late percent in force on that boundary date (due-by plus 30, 60, 90 ... days) of that figure (the rate or bound one), the charges added up; then the floor in force on the claim's date is applied last: the greater of it and the figure plus its charge), due-by (fixed when it was created: its date plus the terms of its person's grade in force on that date — the grade's own, else the sheet's, each as in force on that date), overdue (yes when it is OPEN or PENDING and its due-by plus the grace days of its person's grade in force on its date is before the sheet's today; the late charge's boundaries count from there too), the status buttons (open, settle, void —
// one the claim's status, or the sheet's permission for the user's role, does not allow is refused with the claim's error; a clerk settling above the clerk limit sends the
// claim to PENDING, which keeps any figure a statement bound it to; a steward may void a SETTLED claim within 7 days of
// its settlement by the sheet's today, its frozen figure leaving every total), approve and reject with a reason (a steward's decision
// on a PENDING claim: approve settles it, reject returns it to OPEN; the opener or last editor cannot approve it), review (a steward's
// decision on a REVIEW claim — one whose due was above the review threshold in force on its date when opened — making it OPEN with its number) and an edit button that
// opens a DRAFT claim in the person's claim form (cancel closes it again); editing any other claim, one whose due-by is inside a closed
// period, or one another user's open editor locks, is refused with the claim's error — to change an open claim, void it and
// add a new one. An open editor locks the claim for other users until it is saved or cancelled, or the sheet's today changes. A steward (or the roles the sheet's
// reassign permission allows) moves a DRAFT, OPEN or
// PENDING claim to another person on the sheet with the reassign button (it is listed first under them, live again until
// their next statement); a settled or voided claim, or a move by anyone else, is refused with the claim's error.
// A steward re-settles a SETTLED claim (outside any closed period) with the re-settle button: its due is frozen again at the
// figure it would have today as an open claim bound by no statement, and its log gains a SETTLED line; else it is refused with the claim's error.
// An OPEN claim is deferred to a date after the sheet's today with the defer date and button (a claim that is not OPEN, a date
// not after today or a claim dated inside a closed period is refused with the claim's error): until the sheet's today reaches
// that date it counts in no total, is not overdue, cannot be settled and is left unbound by a statement. Each claim shows
// deferred (yes while it is deferred) and the date it was deferred until.
// A steward waives an OPEN or PENDING claim's late charge with the waive button: it bears no late charge from then on,
// whatever its overdue state. Waiving by anyone else, a claim in any other status, or one already waived is refused with
// the claim's error. Each claim shows waived (yes or no).
// An OPEN or PENDING claim is reminded with the remind button, once per sheet-today: it shows the sheet's today as its reminded
// date (the last day a reminder was written, blank if never). A second reminder the same day, or a claim in any other status,
// is refused with the claim's error.
// A steward re-dates an OPEN or PENDING claim with the new date and re-date button: its due-by stays as it was fixed, a figure a
// statement bound it to stays bound, and, as its period goes by its due-by, it keeps its period and number. A date after the
// person's retirement, a claim whose due-by is inside a closed period, or a re-dating by anyone but a steward, is refused with the claim's error.
// Each claim shows its proofs count, and how many are receipts and photos, raised by one with the proof button (of the
// kind chosen beside it; a receipt when none is chosen) (a SETTLED or VOID claim, or one dated inside a closed
// period, is refused with the claim's error). A claim whose due is above the proof threshold in force on its date cannot be
// settled or approved while its proofs count is 0, nor, while the sheet's proof kinds are set, without at least one proof of
// each listed kind: that is refused with the claim's error, before the clerk limit is considered.
// An OPEN claim is split with the split amount (more than 0.00 and less than its amount) and button: it keeps the rest
// of its amount and loses any figure a statement bound it to, and a new OPEN claim with the split-off amount, the same
// date and note, the next number, opened by the user and its due-by fixed now is listed under the person, showing the
// original's number as split-from. A PENDING, deferred or settled claim, one dated inside a closed period, or a split by a
// role that may not open a claim, is refused with the claim's error.
// Each claim shows its status log, oldest first: one line per status it has had, with who set it, the sheet's today then and its due then.
// A claim is commented with the comment text and button (an empty comment is refused with the claim's error); its comments
// are shown oldest first, each as "<user> on <date>: <text>", the date being the sheet's today when it was written.
// A DRAFT claim takes lines with the line label, quantity and unit amount and the add-line button; its amount is then the
// quantities times the unit amounts added up, and its lines are shown oldest first as "<label> <qty> x <unit>". Adding a line
// counts as editing the claim: a claim that is not a DRAFT, in a closed period or locked by another user is refused with
// the claim's error, as is a blank label, a quantity that is not a whole number of at least 1, or a unit with more than two decimals.
// A DRAFT claim is deleted with the delete button (any other status, or a claim whose due-by is inside a closed period, is
// refused with the claim's error): it is DELETED, counts nowhere and cannot be edited, opened or voided. On the same
// sheet-today it was deleted the restore button makes it a DRAFT again; on a later day that is refused with the claim's error.
export function ClaimList({
  claims,
  people,
  rateOn,
}: {
  claims: Claim[];
  people: Person[];
  rateOn: (claim: Claim) => string | undefined;
}) {
  return (
    <ul data-testid="claims">
      {claims.map((c) => (
        <ClaimRow key={c.id} claim={c} people={people} rate={rateOn(c)} />
      ))}
    </ul>
  );
}

function ClaimRow({ claim: c, people, rate }: { claim: Claim; people: Person[]; rate: string | undefined }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const [editing, setEditing] = useSearchParam('editClaim', asString);
  const queryClient = useQueryClient();
  const period = periodOf(usePeriods(sheet), dueBy(c));
  const floor = useFloor(sheet);
  const today = useSheetToday(sheet);
  const latePct = useLatePct(sheet);
  const grace = useGrace(sheet);
  const policy = usePolicy(sheet);
  const settings = useSettings(sheet);

  const status = useMutation({
    mutationFn: ({ action, reason }: { action: 'open' | 'settle' | 'void' | 'approve' | 'reject' | 'delete' | 'restore' | 'review'; reason?: string }) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/${encodeURIComponent(c.id)}/status`,
        { action, user, reason },
      ),
    onSuccess: (_, { action }) => {
      if (action === 'reject') setReason('');
      if (action === 'void') setVoidReason('');
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
  });

  const [reason, setReason] = useState('');
  const [voidReason, setVoidReason] = useState('');
  const voidReasons = useVoidReasons(sheet);
  const [toPersonId, setToPersonId] = useState('');

  const reassign = useMutation({
    mutationFn: (to: string) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/${encodeURIComponent(c.id)}/reassign`,
        { toPersonId: to, user },
      ),
    onSuccess: () => {
      setToPersonId('');
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
    },
  });

  const resettle = useMutation({
    mutationFn: () =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/${encodeURIComponent(c.id)}/resettle`,
        { user },
      ),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]),
  });

  const [deferUntil, setDeferUntil] = useState('');

  const defer = useMutation({
    mutationFn: (until: string) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/${encodeURIComponent(c.id)}/defer`,
        { until, user },
      ),
    onSuccess: () => {
      setDeferUntil('');
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
    },
  });

  const waive = useMutation({
    mutationFn: () =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/${encodeURIComponent(c.id)}/waive`,
        { user },
      ),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]),
  });

  const remind = useMutation({
    mutationFn: () =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/${encodeURIComponent(c.id)}/remind`,
        { user },
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });

  const [newDate, setNewDate] = useState('');

  const redate = useMutation({
    mutationFn: (date: string) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/${encodeURIComponent(c.id)}/redate`,
        { date, user },
      ),
    onSuccess: () => {
      setNewDate('');
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
    },
  });

  // The kind of the next proof: "" when none is chosen (a receipt).
  const [proofKind, setProofKind] = useState('');

  const proof = useMutation({
    mutationFn: (kind: string) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/${encodeURIComponent(c.id)}/proof`,
        { user, kind },
      ),
    onSuccess: () => {
      setProofKind('');
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
  });

  const [splitAmount, setSplitAmount] = useState('');

  const split = useMutation({
    mutationFn: (amount: string) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/${encodeURIComponent(c.id)}/split`,
        { amount, user },
      ),
    onSuccess: () => {
      setSplitAmount('');
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
    },
  });

  const [commentText, setCommentText] = useState('');

  const comment = useMutation({
    mutationFn: (text: string) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/${encodeURIComponent(c.id)}/comment`,
        { text, user },
      ),
    onSuccess: () => {
      setCommentText('');
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
  });

  const [lineLabel, setLineLabel] = useState('');
  const [lineQty, setLineQty] = useState('');
  const [lineUnit, setLineUnit] = useState('');

  const line = useMutation({
    mutationFn: (l: { label: string; qty: string; unit: string }) =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/${encodeURIComponent(c.id)}/line`,
        { ...l, user },
      ),
    onSuccess: () => {
      setLineLabel('');
      setLineQty('');
      setLineUnit('');
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
    },
  });

  const lock = useMutation({
    mutationFn: () =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/${encodeURIComponent(c.id)}/lock`,
        { user },
      ),
    onSuccess: () => setEditing(c.id),
  });

  const unlock = useMutation({
    mutationFn: () =>
      postJson<Claim>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/${encodeURIComponent(c.id)}/unlock`,
        { user },
      ),
  });

  const [editRefused, setEditRefused] = useState(false);

  const [inlineAmount, setInlineAmount] = useState('');

  // The amount changed in the row: the claim form's save, keeping the claim's other values.
  const inline = useMutation({
    mutationFn: (amount: string) =>
      putJson<Claim>(`/api/sheets/${encodeURIComponent(sheet)}/claims/${encodeURIComponent(c.id)}`, {
        amount,
        date: c.date,
        note: c.note,
        corrects: c.corrects,
        ref: c.ref,
        tags: c.tags ?? '',
        user,
      }),
    onSuccess: () => {
      setInlineAmount('');
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
    },
  });

  // The next action on the row removes an old error.
  const act = (run: () => void) => {
    status.reset();
    reassign.reset();
    resettle.reset();
    defer.reset();
    waive.reset();
    remind.reset();
    redate.reset();
    proof.reset();
    split.reset();
    comment.reset();
    line.reset();
    lock.reset();
    inline.reset();
    setEditRefused(false);
    run();
  };

  // What the screen already knows is refused at once (the claim form must not stay in adding mode
  // while a refusal is on its way); the server takes the lock or refuses what the screen could not see.
  const knownRefused = () =>
    c.status !== 'DRAFT' ||
    period?.closed === true ||
    (c.lockedBy ?? '') !== '' && c.lockedBy !== user ||
    (settings?.roles.some((r) => r.user === user && r.role === 'viewer') === true &&
      // a delegation covering the sheet's today makes the user a steward, as the server judges it
      !(settings?.delegations ?? []).some((d) => d.user === user && today !== undefined && d.from <= today && today <= d.to));
  const edit = () =>
    act(() => {
      if (knownRefused()) setEditRefused(true);
      else lock.mutate();
    });

  const cancel = () =>
    act(() => {
      setEditing(undefined);
      unlock.mutate();
    });

  return (
    <li data-testid={`claim-row-${c.id}`}>
      <ClaimSelect claimId={c.id} />{' '}
      <span data-testid="claim-amount">{c.amount}</span>{' '}
      <span data-testid="claim-date">{c.date}</span>{' '}
      <span data-testid="claim-note">{c.note}</span>{' '}
      <span data-testid="claim-ref">{c.ref}</span>{' '}
      <span data-testid="claim-tags">{tagList(c.tags)}</span>{' '}
      <span data-testid="claim-status">{c.status}</span>{' '}
      <span data-testid="claim-number">{c.number}</span>{' '}
      <span data-testid="claim-due">{showDue(claimDue(c, rate, floor, { pct: latePct, today, grace }, policy))}</span>{' '}
      <span data-testid="claim-due-by">{dueBy(c)}</span>{' '}
      <span data-testid="claim-review-late">{isReviewLate(c, today, settings?.reviewSla) ? 'yes' : 'no'}</span>{' '}
      <span data-testid="claim-overdue">{isOverdue(c, today, grace) ? 'yes' : 'no'}</span>{' '}
      <span data-testid="claim-opened-by">{c.openedBy}</span>{' '}
      <span data-testid="claim-period">{period?.name ?? ''}</span>{' '}
      <span data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</span>{' '}
      <span data-testid="claim-corrects">{c.corrects}</span>{' '}
      <span data-testid="claim-deferred">{deferredShown(c, today) ? 'yes' : 'no'}</span>{' '}
      <span data-testid="claim-deferred-until">{c.deferredUntil ?? ''}</span>{' '}
      <span data-testid="claim-waived">{c.waived ? 'yes' : 'no'}</span>{' '}
      <span data-testid="claim-batch">{c.batch ?? ''}</span>{' '}
      <span data-testid="claim-split-from">{c.splitFrom ?? ''}</span>{' '}
      <span data-testid="claim-late-entry">{c.lateEntry ? 'yes' : 'no'}</span>{' '}
      <span data-testid="claim-reminded">{c.remindedOn ?? ''}</span>{' '}
      <span data-testid="claim-proofs">{c.proofs ?? '0'}</span>{' '}
      (receipts <span data-testid="claim-proofs-receipt">{c.proofReceipts ?? '0'}</span>, photos{' '}
      <span data-testid="claim-proofs-photo">{c.proofPhotos ?? '0'}</span>){' '}
      <ol data-testid="claim-log">
        {(c.log ?? []).map((line, i) => (
          <li key={i} data-testid="claim-log-line">
            {line}
          </li>
        ))}
      </ol>{' '}
      <ClaimTimeline claimId={c.id} />{' '}
      <ol data-testid="claim-comments">
        {(c.comments ?? []).map((line, i) => (
          <li key={i} data-testid="claim-comments-line">
            {line}
          </li>
        ))}
      </ol>{' '}
      <input
        data-testid="claim-comment-text"
        value={commentText}
        placeholder="Comment"
        onChange={(e) => act(() => setCommentText(e.target.value))}
      />{' '}
      <button type="button" data-testid="claim-comment-add" onClick={() => act(() => comment.mutate(commentText))}>
        Comment
      </button>{' '}
      <ol data-testid="claim-lines">
        {(c.lines ?? []).map((l, i) => (
          <li key={i} data-testid="claim-lines-line">
            {l}
          </li>
        ))}
      </ol>{' '}
      <input
        data-testid="claim-line-label"
        value={lineLabel}
        placeholder="Line label"
        onChange={(e) => act(() => setLineLabel(e.target.value))}
      />{' '}
      <input
        data-testid="claim-line-qty"
        value={lineQty}
        placeholder="Qty"
        inputMode="numeric"
        onChange={(e) => act(() => setLineQty(e.target.value))}
      />{' '}
      <input
        data-testid="claim-line-unit"
        value={lineUnit}
        placeholder="Unit amount"
        inputMode="decimal"
        onChange={(e) => act(() => setLineUnit(e.target.value))}
      />{' '}
      <button
        type="button"
        data-testid="claim-line-add"
        onClick={() => act(() => line.mutate({ label: lineLabel, qty: lineQty, unit: lineUnit }))}
      >
        Add line
      </button>{' '}
      <select
        data-testid="claim-proof-kind"
        value={proofKind}
        onChange={(e) => act(() => setProofKind(e.target.value))}
      >
        <option value="">Proof kind (receipt)</option>
        <option value="receipt">receipt</option>
        <option value="photo">photo</option>
      </select>{' '}
      <button type="button" data-testid="claim-proof" onClick={() => act(() => proof.mutate(proofKind))}>
        Add proof
      </button>{' '}
      <button type="button" data-testid="claim-open" onClick={() => act(() => status.mutate({ action: 'open' }))}>
        Open
      </button>{' '}
      <button type="button" data-testid="claim-settle" onClick={() => act(() => status.mutate({ action: 'settle' }))}>
        Settle
      </button>{' '}
      <button type="button" data-testid="claim-resettle" onClick={() => act(() => resettle.mutate())}>
        Re-settle
      </button>{' '}
      {voidReasons.length > 0 && (
        <>
          <select
            data-testid="claim-void-reason"
            value={voidReason}
            onChange={(e) => act(() => setVoidReason(e.target.value))}
          >
            <option value="">Void reason…</option>
            {voidReasons.map((r) => (
              <option key={r} value={r}>{r}</option>
            ))}
          </select>{' '}
        </>
      )}
      <button
        type="button"
        data-testid="claim-void"
        onClick={() => act(() => status.mutate(voidReasons.length > 0 ? { action: 'void', reason: voidReason } : { action: 'void' }))}
      >
        Void
      </button>{' '}
      <button type="button" data-testid="claim-delete" onClick={() => act(() => status.mutate({ action: 'delete' }))}>
        Delete
      </button>{' '}
      <button type="button" data-testid="claim-restore" onClick={() => act(() => status.mutate({ action: 'restore' }))}>
        Restore
      </button>{' '}
      <button type="button" data-testid="claim-review" onClick={() => act(() => status.mutate({ action: 'review' }))}>
        Review
      </button>{' '}
      <button type="button" data-testid="claim-approve" onClick={() => act(() => status.mutate({ action: 'approve' }))}>
        Approve
      </button>{' '}
      <input
        data-testid="claim-reject-reason"
        value={reason}
        placeholder="Reason to reject"
        onChange={(e) => act(() => setReason(e.target.value))}
      />{' '}
      <button type="button" data-testid="claim-reject" onClick={() => act(() => status.mutate({ action: 'reject', reason }))}>
        Reject
      </button>{' '}
      <input
        type="date"
        data-testid="claim-defer-date"
        value={deferUntil}
        onChange={(e) => act(() => setDeferUntil(e.target.value))}
      />{' '}
      <button type="button" data-testid="claim-defer" onClick={() => act(() => defer.mutate(deferUntil))}>
        Defer
      </button>{' '}
      <button type="button" data-testid="claim-waive" onClick={() => act(() => waive.mutate())}>
        Waive late charge
      </button>{' '}
      <button type="button" data-testid="claim-remind" onClick={() => act(() => remind.mutate())}>
        Remind
      </button>{' '}
      <input
        type="date"
        data-testid="claim-date-new"
        value={newDate}
        onChange={(e) => act(() => setNewDate(e.target.value))}
      />{' '}
      <button type="button" data-testid="claim-redate" onClick={() => act(() => redate.mutate(newDate))}>
        Re-date
      </button>{' '}
      <input
        data-testid="claim-split-amount"
        value={splitAmount}
        placeholder="Amount to split off"
        onChange={(e) => act(() => setSplitAmount(e.target.value))}
      />{' '}
      <button type="button" data-testid="claim-split" onClick={() => act(() => split.mutate(splitAmount))}>
        Split
      </button>{' '}
      <select
        data-testid="claim-reassign-to"
        value={toPersonId}
        onChange={(e) => act(() => setToPersonId(e.target.value))}
      >
        <option value="">Move to…</option>
        {people
          .filter((p) => p.id !== c.personId)
          .map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
      </select>{' '}
      <button type="button" data-testid="claim-reassign" onClick={() => act(() => reassign.mutate(toPersonId))}>
        Reassign
      </button>{' '}
      <input
        data-testid="claim-inline-amount"
        value={inlineAmount}
        placeholder="Amount"
        inputMode="decimal"
        onChange={(e) => act(() => setInlineAmount(e.target.value))}
      />{' '}
      <button type="button" data-testid="claim-inline-save" onClick={() => act(() => inline.mutate(inlineAmount.trim()))}>
        Save amount
      </button>{' '}
      {editing === c.id ? (
        <button type="button" data-testid="claim-cancel" onClick={cancel}>
          Cancel
        </button>
      ) : (
        <button type="button" data-testid="claim-edit" onClick={edit}>
          Edit
        </button>
      )}
      {(lock.isError || editRefused) && (
        <span data-testid="claim-error" role="alert">
          {' '}
          Not allowed: only a DRAFT claim outside any closed period can be edited, and not while another user's editor has it open; to change a {c.status} claim, void it and add a new one.
        </span>
      )}
      {inline.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}
          Not allowed: only a DRAFT claim outside any closed period can be edited, not by a viewer and not while another user's editor has it open, and the amount must have at most two decimals (a negative amount is a correction that must name a SETTLED claim of the same person).
        </span>
      )}
      {status.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}
          Not allowed: a {c.status} claim cannot make that status change (a deferred claim cannot be settled until its deferred-until date, the user who opened a claim cannot settle or approve it, a claim whose due is above the proof threshold needs a proof, and one of each of the sheet's proof kinds, to be settled or approved, only a steward can approve or reject a PENDING claim and a rejection needs a reason, only a DRAFT claim can be deleted and a DELETED one restored only on the sheet-today it was deleted, no claim whose due-by is inside a closed period can change status, and a claim cannot be opened while its person already has as many OPEN or PENDING claims in its period as the period's cap).
        </span>
      )}
      {resettle.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}
          Not allowed: only a steward can re-settle a claim, only a SETTLED one, and not one whose due-by is inside a closed period.
        </span>
      )}
      {proof.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}
          Not allowed: a SETTLED or VOID claim, or one whose due-by is inside a closed period, cannot take a proof.
        </span>
      )}
      {defer.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}
          Not allowed: only an OPEN claim can be deferred, only to a date after the sheet's today, and not one whose due-by is inside a closed period.
        </span>
      )}
      {waive.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}
          Not allowed: only a steward can waive a claim's late charge, only an OPEN or PENDING claim's, and only once.
        </span>
      )}
      {remind.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}
          Not allowed: only an OPEN or PENDING claim can be reminded, and only once per sheet-today.
        </span>
      )}
      {redate.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}
          Not allowed: only a steward can re-date a claim, only an OPEN or PENDING one, not to a date after the person's retirement, and not one whose due-by is inside a closed period.
        </span>
      )}
      {comment.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}
          Not allowed: a comment needs some text, and a viewer cannot comment a claim.
        </span>
      )}
      {line.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}
          Not allowed: only a DRAFT claim outside any closed period can take a line, not by a viewer and not while another user's editor has it open; a line needs a label, a whole quantity of at least 1 and a unit amount with at most two decimals.
        </span>
      )}
      {split.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}
          Not allowed: only an OPEN claim that is not deferred and whose due-by is not inside a closed period can be split, by a role that may open a claim, and the amount split off must be more than 0.00 and less than the claim's amount.
        </span>
      )}
      {reassign.isError && (
        <span data-testid="claim-error" role="alert">
          {' '}
          Not allowed: only a steward can move a claim, and only a DRAFT, OPEN or PENDING claim can move to another person.
        </span>
      )}
    </li>
  );
}
