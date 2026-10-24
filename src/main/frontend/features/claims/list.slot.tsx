import { useEffect, useState, type ReactNode } from 'react';
import { useIsMutating, useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { gradeOn, peopleQueryKey, usePeople, type Person } from '../people/people';
import { chargedOf, useRates } from '../rates/rates';
import { periodOn, usePeriods } from '../periods/periods';
import { useSettings } from '../roles/roles';
import { claimsPath, claimsQueryKey, dueByOf, isDeferred, isOverdue, personClaimsPath, useClaims, useEditingClaim, useSelectedClaims, type Claim } from './claims';

// A person's claims, newest first, with the claim form that adds one — or, while one of their
// claims' editor is open, saves its changes.
function ClaimsHeader() {
  return <th>Claims</th>;
}

function PersonClaims({ person }: { person: Person }) {
  const [sheet] = useCurrentSheet();
  const [user] = useCurrentUser();
  const [editing, setEditing] = useEditingClaim();
  // Bumped each time the editor is submitted: the edited claim's lock error then goes.
  const [submits, setSubmits] = useState(0);
  const { selected, toggle } = useSelectedClaims();
  const claims = useClaims();
  const rates = useRates();
  const periods = usePeriods();
  const settings = useSettings();
  const today = useSheetToday();
  if (!sheet) return null;
  const mine = (claims.data ?? []).filter((c) => c.personId === person.id);
  const claim = editing ? mine.find((c) => String(c.id) === editing) : undefined;
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
            <span data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</span>{' '}
            <span data-testid="claim-opened-by">{c.openedBy ?? ''}</span>{' '}
            <span data-testid="claim-due-by">{dueByOf(c)}</span>{' '}
            <span data-testid="claim-corrects">{c.corrects ?? ''}</span>{' '}
            <span data-testid="claim-overdue">{isOverdue(c, today) ? 'yes' : 'no'}</span>{' '}
            <span data-testid="claim-deferred">{isDeferred(c, today) ? 'yes' : 'no'}</span>{' '}
            <span data-testid="claim-deferred-until">{c.deferredUntil ?? ''}</span>{' '}
            <span data-testid="claim-proofs">{c.proofs}</span>{' '}
            <span data-testid="claim-split-from">{c.splitFrom ?? ''}</span>{' '}
            <span data-testid="claim-late-entry">{c.lateEntry ? 'yes' : 'no'}</span>{' '}
            <span data-testid="claim-reminded">{c.remindedOn ?? ''}</span>{' '}
            <ClaimStatusActions key={`${sheet}:${c.id}`} sheet={sheet} claim={c} onEdit={() => setEditing(String(c.id))}
              editorSubmits={claim?.id === c.id ? submits : 0}>
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
            <ol data-testid="claim-log">
              {(c.log ?? []).map((line, i) => (
                <li key={i} data-testid="claim-log-line">{line}</li>
              ))}
            </ol>
          </li>
        ))}
      </ul>
      {/* Keyed by the claim being edited, so opening an editor fills the form with its values. */}
      <ClaimForm key={`${sheet}:${claim?.id ?? ''}`} sheet={sheet} person={person} claim={claim}
        onSubmitted={() => setSubmits((n) => n + 1)} />
    </td>
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

// A claim's status and the buttons that change it. A change its status does not allow is refused
// by the server and shows the claim's error, as is settling or approving a claim the signed-in user opened, a
// non-steward approving or rejecting a PENDING claim, an open, settle or void by a role the permission matrix does
// not allow, or rejecting one without a reason (typed beside the reject
// button). A clerk settling a claim whose due is above the clerk limit leaves it PENDING. The error goes as soon as
// the next action on the row is taken. Only a DRAFT can be edited: the edit button of any other claim, or of a claim whose due-by is in a
// closed period, shows the claim's error instead of opening its editor (an open claim is voided and added anew). Opening a
// DRAFT's editor locks the claim for other users until it is saved or cancelled, or the sheet's today changes: another
// user's edit button on a locked claim (and a viewer's) shows the claim's error, and their editor cannot save. A role the permission matrix allows (a steward, by the standing
// rule) moves a DRAFT, OPEN or PENDING
// claim to another person (picked beside the move button); anyone else moving it, or moving a SETTLED or VOID claim,
// shows the claim's error. A steward voids a SETTLED claim within 7 days of its settlement (its frozen due leaves
// the totals); anyone else, or a later day, shows the claim's error. A steward re-settles a SETTLED claim at the figure it would have today as an open claim
// bound by no statement; anyone else, or any other status, shows the claim's error. An OPEN claim is deferred until
// a date after the sheet's today (typed beside the defer button); any other status, a date not after today, or a claim
// whose due-by is in a closed period shows the claim's error. A deferred claim cannot be settled. The proof button raises the claim's
// proofs count by one; on a SETTLED or VOID claim it shows the claim's error. A claim whose due is above the proof
// threshold in force on its date cannot be settled or approved while it has no proof: that shows the claim's error.
// An OPEN claim splits off an amount (typed beside the split button; more than 0.00 and less than its amount) into a
// new OPEN claim; any other status, a deferred claim, a claim whose due-by is in a closed period or an amount out of range shows the
// claim's error. A steward re-dates an OPEN or PENDING claim (the date typed beside the re-date button); anyone
// else, any other status, a claim whose due-by is in a closed period, or a new date after the person's retirement shows
// the claim's error. Re-dating keeps the due-by, so the claim keeps its period and number. An OPEN or PENDING claim is
// reminded once per sheet's today (its reminded date becomes today); a second press that day, or any other status, shows
// the claim's error.
function ClaimStatusActions({ sheet, claim, onEdit, editorSubmits, children }: {
  sheet: string; claim: Claim; onEdit: () => void; editorSubmits: number; children: ReactNode;
}) {
  const [user] = useCurrentUser();
  const queryClient = useQueryClient();
  const [reason, setReason] = useState('');
  const change = useMutation({
    mutationFn: ({ action, reason }: { action: string; reason?: string }) =>
      putJson<Claim>(`${claimsPath(sheet)}/${claim.id}/${action}`
        + (reason === undefined ? '' : `?reason=${encodeURIComponent(reason)}`), undefined, user),
    onSuccess: (_, { action }) => {
      if (action === 'reject') setReason('');
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
  const today = useSheetToday();
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
  const proof = useMutation({
    mutationFn: () => putJson<Claim>(`${claimsPath(sheet)}/${claim.id}/proof`, undefined, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) }),
  });
  const submitProof = useSubmitOnce(proof);
  const remind = useMutation({
    mutationFn: () => putJson<Claim>(`${claimsPath(sheet)}/${claim.id}/remind`, undefined, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet) }),
  });
  const submitRemind = useSubmitOnce(remind);
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
  const [editRefused, setEditRefused] = useState(false);
  const lock = useMutation({
    mutationKey: claimLockKey(sheet, claim.id),
    mutationFn: () => putJson<Claim>(`${claimsPath(sheet)}/${claim.id}/lock`, undefined, user),
  });
  const submitLock = useSubmitOnce(lock);
  // Submitting the claim's editor is the next action on it: a refused lock's error goes.
  useEffect(() => {
    if (editorSubmits > 0) lock.reset();
  }, [editorSubmits]);
  const action = change.variables?.action;
  const periods = usePeriods();
  const closed = !!periodOn(periods.data, dueByOf(claim))?.closed;
  return (
    <>
      <span data-testid="claim-status">{claim.status}</span>{' '}
      <input
        data-testid="claim-reject-reason"
        placeholder="Reason to reject"
        value={reason}
        onChange={(e) => { setReason(e.target.value); change.reset(); defer.reset(); redate.reset(); split.reset(); proof.reset(); remind.reset(); setEditRefused(false); lock.reset(); }}
      />{' '}
      {ACTIONS.map(({ action, label }) => (
        <button
          key={action}
          type="button"
          data-testid={`claim-${action}`}
          disabled={change.isPending}
          onClick={() => {
            setEditRefused(false); lock.reset();
            move.reset();
            defer.reset(); redate.reset(); split.reset();
            proof.reset(); remind.reset();
            submitChange(action === 'reject' ? { action, reason } : { action });
          }}
        >
          {label}
        </button>
      ))}{' '}
      <select
        data-testid="claim-reassign-to"
        value={target ? String(target.id) : ''}
        onChange={(e) => { setMoveTo(e.target.value); move.reset(); change.reset(); defer.reset(); redate.reset(); split.reset(); proof.reset(); remind.reset(); setEditRefused(false); lock.reset(); }}
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
          defer.reset(); redate.reset(); split.reset();
          proof.reset(); remind.reset();
          setEditRefused(false); lock.reset();
          if (target) submitMove(target.id);
        }}
      >
        Move
      </button>{' '}
      <input
        data-testid="claim-defer-date"
        placeholder="Defer until YYYY-MM-DD"
        value={deferTo}
        onChange={(e) => { setDeferTo(e.target.value); defer.reset(); redate.reset(); split.reset(); change.reset(); move.reset(); proof.reset(); remind.reset(); setEditRefused(false); lock.reset(); }}
      />{' '}
      <button
        type="button"
        data-testid="claim-defer"
        disabled={defer.isPending}
        onClick={() => {
          change.reset();
          move.reset();
          proof.reset(); remind.reset();
          split.reset();
          setEditRefused(false); lock.reset();
          submitDefer(deferTo);
        }}
      >
        Defer
      </button>{' '}
      <input
        data-testid="claim-date-new"
        placeholder="Re-date to YYYY-MM-DD"
        value={redateTo}
        onChange={(e) => { setRedateTo(e.target.value); redate.reset(); defer.reset(); split.reset(); change.reset(); move.reset(); proof.reset(); remind.reset(); setEditRefused(false); lock.reset(); }}
      />{' '}
      <button
        type="button"
        data-testid="claim-redate"
        disabled={redate.isPending}
        onClick={() => {
          change.reset();
          move.reset();
          defer.reset();
          proof.reset(); remind.reset();
          split.reset();
          setEditRefused(false); lock.reset();
          submitRedate(redateTo);
        }}
      >
        Re-date
      </button>{' '}
      <button
        type="button"
        data-testid="claim-proof"
        disabled={proof.isPending}
        onClick={() => {
          change.reset();
          move.reset();
          defer.reset(); redate.reset(); split.reset();
          setEditRefused(false); lock.reset();
          submitProof(undefined);
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
          defer.reset(); redate.reset(); split.reset();
          proof.reset();
          setEditRefused(false); lock.reset();
          submitRemind(undefined);
        }}
      >
        Remind
      </button>{' '}
      <input
        data-testid="claim-split-amount"
        placeholder="Amount to split off"
        inputMode="decimal"
        value={splitAmount}
        onChange={(e) => { setSplitAmount(e.target.value); split.reset(); change.reset(); move.reset(); defer.reset(); redate.reset(); proof.reset(); remind.reset(); setEditRefused(false); lock.reset(); }}
      />{' '}
      <button
        type="button"
        data-testid="claim-split"
        disabled={split.isPending}
        onClick={() => {
          change.reset();
          move.reset();
          defer.reset(); redate.reset();
          proof.reset(); remind.reset();
          setEditRefused(false); lock.reset();
          submitSplit(splitAmount);
        }}
      >
        Split
      </button>{' '}
      <span onClickCapture={() => { change.reset(); move.reset(); defer.reset(); redate.reset(); split.reset(); proof.reset(); remind.reset(); setEditRefused(false); lock.reset(); }}>
        <button
          type="button"
          data-testid="claim-edit"
          onClick={() => {
            if (claim.status !== 'DRAFT' || closed) {
              setEditRefused(true);
            } else {
              onEdit();
              submitLock(undefined);
            }
          }}
        >
          Edit
        </button>
        {children}
      </span>
      {(change.isError || move.isError || defer.isError || redate.isError || proof.isError || remind.isError || split.isError || editRefused || lock.isError) && (
        <span data-testid="claim-error">
          {lock.isError
            ? lock.error instanceof HttpError && lock.error.status === 423
              ? 'Another user is editing this claim; it cannot be edited until they save or cancel.'
              : lock.error instanceof HttpError && lock.error.status === 403
              ? 'A viewer cannot edit a claim.'
              : closed
              ? 'This claim is due by a date in a closed period, so it cannot be edited.'
              : `Only a draft claim can be edited; void this ${claim.status} claim and add a new one.`
            : split.isError
            ? closed
              ? 'This claim is due by a date in a closed period, so it cannot be split.'
              : claim.status !== 'OPEN' || isDeferred(claim, today)
              ? `Only an open claim that is not deferred can be split; this one is ${isDeferred(claim, today) ? 'deferred' : claim.status}.`
              : `The amount to split off must be more than 0.00 and less than the claim's ${claim.amount}.`
            : remind.isError
            ? remind.error instanceof HttpError && remind.error.status === 403
              ? 'A viewer cannot remind a claim.'
              : claim.status !== 'OPEN' && claim.status !== 'PENDING'
              ? `Only an open or pending claim can be reminded; this one is ${claim.status}.`
              : `This claim was already reminded today (${today ?? ''}).`
            : proof.isError
            ? `A ${claim.status} claim cannot gain a proof.`
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
            : (action === 'approve' || action === 'reject') && change.error instanceof HttpError && change.error.status === 403
            ? 'Only a steward can approve or reject a pending claim.'
            : action === 'void' && claim.status === 'SETTLED' && change.error instanceof HttpError && change.error.status === 403
            ? 'Only a steward can void a settled claim.'
            : (action === 'open' || action === 'settle' || action === 'void') && change.error instanceof HttpError && change.error.status === 403
            ? `Your role is not allowed to ${action} a claim.`
            : action === 'void' && claim.status === 'SETTLED'
            ? 'A settled claim can be voided only within 7 days of its settlement.'
            : action === 'resettle' && change.error instanceof HttpError && change.error.status === 403
            ? 'Only a steward can re-settle a claim.'
            : action === 'resettle'
            ? `Only a settled claim can be re-settled; this one is ${claim.status}.`
            : (action === 'settle' || action === 'approve') && change.error instanceof HttpError && change.error.status === 422
            ? 'This claim\'s due is above the proof threshold, so it needs a proof before it can be settled or approved.'
            : action === 'settle' && isDeferred(claim, today)
            ? `This claim is deferred until ${claim.deferredUntil}, so it cannot be settled before then.`
            : action === 'settle' && claim.openedBy === user
            ? 'You opened this claim, so someone else must settle it.'
            : action === 'approve' && claim.openedBy === user && claim.status === 'PENDING'
            ? 'You opened this claim, so someone else must approve it.'
            : action === 'reject' && claim.status === 'PENDING' && reason.trim() === ''
            ? 'Give a reason to reject this claim.'
            : `That status change is not allowed for a ${claim.status} claim.`}
        </span>
      )}
    </>
  );
}

function ClaimForm({ sheet, person, claim, onSubmitted }: {
  sheet: string; person: Person; claim?: Claim; onSubmitted: () => void;
}) {
  const [user] = useCurrentUser();
  const [, setEditing] = useEditingClaim();
  const today = useSheetToday();
  const [amount, setAmount] = useState(claim?.amount ?? '');
  const [date, setDate] = useState(claim?.date ?? '');
  const [note, setNote] = useState(claim?.note ?? '');
  const [corrects, setCorrects] = useState(claim?.corrects == null ? '' : String(claim.corrects));
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (body: { amount: string; date: string; note: string; corrects: string }) =>
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
      }
    },
  });
  const submit = useSubmitOnce(save);
  // The editor cannot save until the edit button's lock has answered (taken, or refused).
  const locking = useIsMutating({ mutationKey: claimLockKey(sheet, claim?.id ?? 0) }) > 0 && !!claim;

  return (
    <form
      data-testid="claim-form"
      onSubmit={(e) => {
        e.preventDefault();
        onSubmitted();
        submit({ amount, date, note, corrects });
      }}
    >
      <input
        data-testid="claim-form-amount"
        placeholder="Amount"
        inputMode="decimal"
        value={amount}
        onChange={(e) => { setAmount(e.target.value); save.reset(); }}
      />
      <input
        data-testid="claim-form-date"
        placeholder={today ?? 'YYYY-MM-DD'}
        value={date}
        onChange={(e) => { setDate(e.target.value); save.reset(); }}
      />
      <input
        data-testid="claim-form-note"
        placeholder="Note"
        value={note}
        onChange={(e) => { setNote(e.target.value); save.reset(); }}
      />
      <input
        data-testid="claim-form-corrects"
        placeholder="Corrects claim no."
        inputMode="numeric"
        value={corrects}
        onChange={(e) => { setCorrects(e.target.value); save.reset(); }}
      />
      <button type="submit" data-testid="claim-form-submit" disabled={save.isPending || locking}>
        {claim ? 'Save claim' : 'Add claim'}
      </button>
      {save.isError && (
        <span data-testid="claim-form-error">
          {save.error instanceof HttpError && save.error.status === 423 && 'Another user is editing this claim. '}
          The amount must be a number with up to two decimals, and the date a calendar date whose due-by (it plus the
          terms) is in no closed period and not after the person's retirement date; a negative amount is a correction and must name, by
          number, a settled claim of this person (only a correction may); only a draft claim can be edited.
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
