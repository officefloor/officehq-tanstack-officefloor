import { useEffect, useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { gradeOn, heldOn, usePeople, type Person } from '../people/usePeople';
import { periodOf, usePeriods } from '../periods/usePeriods';
import { rateOn, showDue, useRates } from '../rates/useRates';
import { useCap, useFloor, useLate } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheet';
import { useSelectedClaims } from '../bulk/useSelectedClaims';
import {
  AMOUNT,
  CLAIM_NUMBER,
  claimDueCents,
  claimed,
  claimsKey,
  live,
  overdue,
  overdueFigures,
  personDueCents,
  capCents,
  excessCents,
  useClaims,
  type Claim,
} from './useClaims';

const STATUS_REFUSED = "This claim's status cannot change that way (whoever opened a claim cannot settle it).";
const OVER_LIMIT = "A clerk cannot settle a claim whose due is over the sheet's clerk limit.";
const NOT_STEWARD = 'Only a steward can approve a pending claim, and never the user who opened it.';
const NOT_ALLOWED = "Your role may not do that: the sheet's permission matrix decides who opens, settles and voids claims.";
const CANNOT_VOID_SETTLED = 'Only a steward can void a SETTLED claim, and only within 7 days of its settlement (never one dated in a closed period).';
const NO_REASON = 'A rejection needs a reason.';
const NOT_STEWARD_MOVE = "Your role may not reassign a claim (by default only a steward can).";
const CANNOT_MOVE = 'Only a DRAFT, OPEN or PENDING claim can be reassigned to another person.';
const CANNOT_RESETTLE = 'Only a steward can re-settle a claim, and only a SETTLED one dated outside a closed period.';
const NO_TARGET = 'Choose another person to reassign this claim to.';
const CANNOT_DEFER = "Only an OPEN claim dated outside a closed period can be deferred, and only to a date after the sheet's today.";
const CANNOT_REDATE = "Only a steward can re-date a claim, only an OPEN or PENDING one, never in or into a closed period, nor to a date after its person's retirement.";
const DEFERRED = 'A deferred claim cannot be settled until the sheet\'s today reaches its deferred-until date.';
const NEEDS_PROOF = "A claim whose due is above the sheet's proof threshold cannot be settled or approved until it has a proof.";
const CANNOT_PROVE = 'A proof cannot be added to a SETTLED or VOID claim, nor to one dated in a closed period.';
const CANNOT_SPLIT = "Only an OPEN claim (not deferred, nor dated in a closed period) can be split, by a user whose role may open a claim.";
const BAD_SPLIT = "The amount split off must be more than 0.00 and less than the claim's amount, with up to two decimals.";
const NOT_DRAFT = 'Only a DRAFT claim can be edited: to change an open claim, void it and add a new one.';
const LOCKED = 'Another user is editing this claim: it is locked until they save or cancel their editor.';
const CLOSED_EDIT = 'This claim is dated in a closed period: it cannot be edited.';
const VIEWER = 'A viewer can change nothing.';
const ARCHIVED = 'The sheet is archived: nothing on it can be changed.';

/** Why the server refused to open a claim's editor. */
function lockRefusal(e: unknown): string {
  if (!(e instanceof HttpError)) return "Could not open this claim's editor.";
  if (e.body.includes('locked')) return LOCKED;
  if (e.body.includes('viewer')) return VIEWER;
  if (e.body.includes('archived')) return ARCHIVED;
  if (e.body.includes('closed')) return CLOSED_EDIT;
  return NOT_DRAFT;
}

/** A save refused because its editor's lock was not taken. */
class LockRefused extends Error {
  constructor(readonly refusal: { id: number; message: string }) {
    super(refusal.message);
  }
}

const ACTIONS = ['open', 'settle', 'approve', 'reject', 'void'] as const;
type Action = (typeof ACTIONS)[number];
const ACTION_LABELS: Record<Action, string> = { open: 'Open', settle: 'Settle', approve: 'Approve', reject: 'Reject', void: 'Void' };

/**
 * A person's claims: what they have claimed in all and their due-total, the sum of their claims' rounded dues,
 * one form to add a claim or edit the one whose editor is open (only a DRAFT outside a closed period can be edited; an open
 * editor locks its claim for every other user until it is saved or cancelled, or the sheet's today changes), and the claims, newest first, each
 * with its period (the one whose dates contain its date, if any), its due (fixed once it is settled, else bound by the last statement, else at the rate in force on the claim's date, plus its late charge, at least the sheet's floor; in the
 * due-total too), its status, who opened it, whether a statement binds it and the buttons to open, settle, approve, reject (with a reason) or void it (a SETTLED one only by a steward within 7 days of its settlement; its opener cannot settle
 * or approve it), and for a steward to re-settle a SETTLED one at today's figure. A clerk's settle over the clerk limit leaves the claim PENDING until a steward approves
 * or rejects it; while PENDING it shows its live figure, not the one a statement bound it to.
 * Only OPEN, PENDING and SETTLED claims count toward claimed and the due-total, and not the OPEN or
 * PENDING ones dated within one of the person's holds, unless a statement binds them.
 * A claim with a negative amount is a correction: it names, by number, a SETTLED claim of the person
 * that it corrects (refused otherwise), shows that number, and its due has no floor and no late charge.
 * Each claim shows its due-by (fixed when it was added, from the terms of its person's grade) and whether it is overdue (OPEN and
 * due by before the sheet's today); the person shows how many of the claims that count are overdue
 * and their dues added up.
 * A steward can reassign a DRAFT, OPEN or PENDING claim to another person on the sheet: it keeps its
 * number and date, is no longer bound by a statement, and is listed as its new person's newest claim.
 * A steward can re-date an OPEN or PENDING claim (not in or into a closed period, nor after its person's
 * retirement): its due-by and any figure a statement bound stay; its period follows the new date.
 * An OPEN claim can be deferred until a date after the sheet's today: until today reaches it, it shows
 * deferred yes and its deferred-until date, counts in no total, is not overdue and cannot be settled.
 * Each claim shows its proofs count, raised by one with its proof button (not on a SETTLED or VOID claim,
 * nor one dated in a closed period); one whose due is above the sheet's proof threshold on its date
 * cannot be settled or approved while it has none.
 * An OPEN claim can be split: an amount (more than 0.00, less than its amount) goes off it into a new
 * OPEN claim with the same date and note, the next number, opened by the user, which shows the
 * original's number as split-from; the original keeps the rest and is no longer bound by a statement.
 */
function PersonClaims({ user, sheet, person }: { user: string; sheet: string; person: Person }) {
  const queryClient = useQueryClient();
  const { data: all = [] } = useClaims(sheet);
  const claims = all.filter((c) => c.personId === person.id);
  // Their OPEN and PENDING claims dated within a hold count toward neither claimed nor the due-total,
  // unless a statement binds them.
  const liveClaims = live(claims, (_, date) => heldOn(person, date));
  const { data: everyone = [] } = usePeople(sheet);
  const others = everyone.filter((p) => p.id !== person.id);
  const { data: rates = [] } = useRates(sheet);
  const { data: periods = [] } = usePeriods(sheet);
  const today = useSheetToday(sheet);
  const pct = rateOn(rates, gradeOn(person, today), today);
  const floor = useFloor(sheet);
  const lateBy = useLate(sheet);
  const cap = useCap(sheet);
  // The sum of their figures; their due-total is at most the cap, the rest their excess.
  const dueSum = personDueCents(liveClaims, floor, pct, (d) => rateOn(rates, gradeOn(person, d), d), lateBy);
  const late = overdueFigures(liveClaims, lateBy, floor, (d) => rateOn(rates, gradeOn(person, d), d));
  // The claim whose editor this user has open: the one their lock holds, so it stays open across a re-load.
  const editingClaim = claims.find((c) => c.lockedBy === user) ?? null;
  const editing = editingClaim?.id ?? null;
  const [amount, setAmount] = useState('');
  const [date, setDate] = useState('');
  const [note, setNote] = useState('');
  const [corrects, setCorrects] = useState('');
  const [invalid, setInvalid] = useState(false);
  // The claim whose last status change or edit was refused: its row shows the error.
  const [refused, setRefused] = useState<{ id: number; message: string } | null>(null);
  // The claims ticked for a bulk action (the bulk-open panel acts on them).
  const [selected, setSelected] = useSelectedClaims();
  // The reason typed for rejecting each pending claim, by claim id.
  const [reasons, setReasons] = useState<Record<number, string>>({});
  // The date typed for deferring each claim, by claim id.
  const [deferTo, setDeferTo] = useState<Record<number, string>>({});
  // The new date typed for re-dating each claim, by claim id.
  const [redateTo, setRedateTo] = useState<Record<number, string>>({});
  // The person chosen to reassign each claim to, by claim id.
  const [moveTo, setMoveTo] = useState<Record<number, string>>({});
  // The amount typed for splitting each claim, by claim id.
  const [splitOff, setSplitOff] = useState<Record<number, string>>({});
  const base = `/api/sheets/${encodeURIComponent(sheet)}`;

  // Put a claim the server answered with into the sheet's claims, so its lock shows at once.
  const putClaim = (claim: Claim) =>
    queryClient.setQueryData<Claim[]>(claimsKey(sheet), (old) =>
      old?.map((c) =>
        c.id === claim.id
          ? claim
          : claim.lockedBy === user && c.personId === claim.personId && c.lockedBy === user
            ? { ...c, lockedBy: null }
            : c,
      ),
    );

  // The last edit button's lock, once the server has answered: null when it was taken, else the claim and why it was refused.
  const locking = useRef<Promise<{ id: number; message: string } | null>>(Promise.resolve(null));
  const submitOnce = useSubmitOnce();
  const save = useMutation({
    mutationFn: async (c: { id: number | null; amount: string; date: string; note: string; corrects: string }) => {
      // An edit goes only once its editor's lock is taken; a refused lock is shown on the claim.
      const refusal = c.id !== null ? await locking.current : null;
      if (refusal !== null && refusal.id === c.id) throw new LockRefused(refusal);
      const body = { amount: c.amount, date: c.date, note: c.note, corrects: c.corrects, user };
      return c.id === null
        ? postJson<Claim>(`${base}/people/${person.id}/claims`, body)
        : putJson<Claim>(`${base}/claims/${c.id}`, body);
    },
    onSuccess: async (claim, c) => {
      // A saved edit releases the claim's lock, closing its editor.
      if (c.id !== null) putClaim(claim);
      close();
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
    // An edit of a claim no longer a DRAFT, or locked by another user, is refused on the claim itself; the editor stays open.
    onError: async (e, c) => {
      if (e instanceof LockRefused) {
        setRefused(e.refusal);
        return;
      }
      if (c.id !== null && e instanceof HttpError && e.status === 409 && (e.body.includes('DRAFT') || e.body.includes('locked'))) {
        setRefused({ id: c.id, message: e.body.includes('locked') ? LOCKED : NOT_DRAFT });
        await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
      }
    },
  });

  const changeStatus = useMutation({
    mutationFn: (c: { id: number; action: Action; reason?: string }) =>
      postJson<Claim>(`${base}/claims/${c.id}/status`, { action: c.action, user, reason: c.reason }),
    onMutate: () => setRefused(null),
    onSuccess: (_, c) => {
      if (c.action === 'reject') setReasons((r) => ({ ...r, [c.id]: '' }));
    },
    onError: (e, c) =>
      setRefused({
        id: c.id,
        message: !(e instanceof HttpError)
          ? STATUS_REFUSED
          : e.body.includes('void a settled')
            ? CANNOT_VOID_SETTLED
          : e.body.includes('proof threshold')
            ? NEEDS_PROOF
          : e.body.includes('clerk limit')
            ? OVER_LIMIT
            : e.body.includes('deferred')
              ? DEFERRED
            : e.body.includes('Your role')
              ? NOT_ALLOWED
            : e.body.includes('approve')
              ? NOT_STEWARD
              : e.body.includes('reason')
                ? NO_REASON
                : STATUS_REFUSED,
      }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });

  // A steward re-settles a SETTLED claim at the figure it would have today as an unbound open claim.
  const resettle = useMutation({
    mutationFn: (c: { id: number }) => postJson<Claim>(`${base}/claims/${c.id}/resettle`, { user }),
    onMutate: () => setRefused(null),
    onError: (_, c) => setRefused({ id: c.id, message: CANNOT_RESETTLE }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });

  // An OPEN claim is deferred until a date after the sheet's today.
  const defer = useMutation({
    mutationFn: (c: { id: number; until: string }) =>
      postJson<Claim>(`${base}/claims/${c.id}/defer`, { until: c.until, user }),
    onMutate: () => setRefused(null),
    onSuccess: (_, c) => setDeferTo((d) => ({ ...d, [c.id]: '' })),
    onError: (_, c) => setRefused({ id: c.id, message: CANNOT_DEFER }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });

  // A steward re-dates an OPEN or PENDING claim: its due-by and any bound figure stay as they were.
  const redate = useMutation({
    mutationFn: (c: { id: number; date: string }) =>
      postJson<Claim>(`${base}/claims/${c.id}/redate`, { date: c.date, user }),
    onMutate: () => setRefused(null),
    onSuccess: (_, c) => setRedateTo((d) => ({ ...d, [c.id]: '' })),
    onError: (_, c) => setRefused({ id: c.id, message: CANNOT_REDATE }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });

  // A proof raises the claim's proofs count by one.
  const addProof = useMutation({
    mutationFn: (c: { id: number }) => postJson<Claim>(`${base}/claims/${c.id}/proof`, { user }),
    onMutate: () => setRefused(null),
    onError: (_, c) => setRefused({ id: c.id, message: CANNOT_PROVE }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });

  // An amount split off an OPEN claim into a new OPEN claim.
  const split = useMutation({
    mutationFn: (c: { id: number; amount: string }) =>
      postJson<Claim>(`${base}/claims/${c.id}/split`, { amount: c.amount, user }),
    onMutate: () => setRefused(null),
    onSuccess: (_, c) => setSplitOff((s) => ({ ...s, [c.id]: '' })),
    onError: (e, c) =>
      setRefused({
        id: c.id,
        message: e instanceof HttpError && (e.status === 400 || e.body.includes('less than')) ? BAD_SPLIT : CANNOT_SPLIT,
      }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });

  const reassign = useMutation({
    mutationFn: (c: { id: number; toPersonId: number }) =>
      postJson<Claim>(`${base}/claims/${c.id}/reassign`, { toPersonId: c.toPersonId, user }),
    onMutate: () => setRefused(null),
    onSuccess: (_, c) => setMoveTo((m) => ({ ...m, [c.id]: '' })),
    onError: (e, c) =>
      setRefused({
        id: c.id,
        message: e instanceof HttpError && e.body.includes('role')
          ? NOT_STEWARD_MOVE
          : e instanceof HttpError && e.body.includes('retired')
            ? 'That person is retired before this claim\'s date.'
            : e instanceof HttpError && e.body.includes('closed')
              ? 'This claim is dated in a closed period.'
              : CANNOT_MOVE,
      }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });

  // The edit button: the server locks the claim for this user, or refuses (another user's lock, not a
  // DRAFT, dated in a closed period, a viewer) on the claim itself, taking no lock.
  // The editor opens at once, as if locked; a refusal puts the claims back as they were.
  const lock = useMutation({
    mutationFn: (c: Claim) => postJson<Claim>(`${base}/claims/${c.id}/lock`, { user }),
    onMutate: (c) => {
      clearError();
      const before = queryClient.getQueryData<Claim[]>(claimsKey(sheet));
      putClaim({ ...c, lockedBy: user });
      fill(c);
      return { before };
    },
    onSuccess: (claim) => putClaim(claim),
    onError: (e, c, context) => {
      if (context?.before) queryClient.setQueryData(claimsKey(sheet), context.before);
      setRefused({ id: c.id, message: lockRefusal(e) });
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });
  const openEditor = (c: Claim) => {
    // What the screen already knows is refused at once, as before: a claim no longer a DRAFT, or one
    // another user's editor locks. The server decides the rest (a closed period, a viewer).
    const known = c.status !== 'DRAFT' ? NOT_DRAFT : c.lockedBy !== null && c.lockedBy !== user ? LOCKED : null;
    if (known !== null) {
      clearError();
      setRefused({ id: c.id, message: known });
      locking.current = Promise.resolve({ id: c.id, message: known });
      return;
    }
    locking.current = lock.mutateAsync(c).then(
      () => null,
      (e) => ({ id: c.id, message: lockRefusal(e) }),
    );
  };

  // The cancel button: the server releases this user's lock on the claim.
  const unlock = useMutation({
    mutationFn: (c: { id: number }) => postJson<Claim>(`${base}/claims/${c.id}/unlock`, { user }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });

  // An error goes as soon as its form is changed or submitted again, or an editor is opened.
  const clearError = () => {
    save.reset();
    setInvalid(false);
    setRefused(null);
  };
  const close = () => {
    setAmount('');
    setDate('');
    setNote('');
    setCorrects('');
  };
  const fill = (c: Claim) => {
    setAmount(c.amount);
    setDate(c.date);
    setNote(c.note);
    setCorrects(c.corrects === null ? '' : String(c.corrects));
  };
  // The form follows the editor this user's lock holds: filled when it opens (also on a re-load), cleared when it closes.
  useEffect(() => {
    if (editingClaim === null) close();
    else fill(editingClaim);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editing]);
  const cancel = () => {
    clearError();
    if (editingClaim !== null) {
      putClaim({ ...editingClaim, lockedBy: null });
      unlock.mutate({ id: editingClaim.id });
    }
    close();
  };

  return (
    <>
      <td data-testid="person-claimed">{claimed(liveClaims)}</td>
      <td data-testid="person-due-total">{showDue(capCents(dueSum, cap))}</td>
      <td data-testid="person-excess">{showDue(excessCents(dueSum, cap))}</td>
      <td data-testid="person-overdue-count">{late.count}</td>
      <td data-testid="person-overdue-total">{showDue(late.cents)}</td>
      <td>
        <form
          data-testid="claim-form"
          onSubmit={(e) => {
            e.preventDefault();
            // A negative amount is a correction: it must name the number of the claim it corrects.
            if (!AMOUNT.test(amount.trim()) || (amount.trim().startsWith('-') && !CLAIM_NUMBER.test(corrects.trim()))) {
              // Refused here, before anything is sent: the store is untouched.
              save.reset();
              setInvalid(true);
              return;
            }
            setInvalid(false);
            submitOnce(() => save.mutateAsync({ id: editing, amount: amount.trim(), date: date.trim(), note, corrects: corrects.trim() }));
          }}
        >
          <input
            data-testid="claim-form-amount"
            aria-label="Amount"
            placeholder="0.00"
            value={amount}
            onChange={(e) => {
              setAmount(e.target.value);
              clearError();
            }}
          />{' '}
          <input
            data-testid="claim-form-date"
            aria-label="Date"
            placeholder="yyyy-mm-dd"
            value={date}
            onChange={(e) => {
              setDate(e.target.value);
              clearError();
            }}
          />{' '}
          <input
            data-testid="claim-form-note"
            aria-label="Note"
            placeholder="note"
            value={note}
            onChange={(e) => {
              setNote(e.target.value);
              clearError();
            }}
          />{' '}
          <input
            data-testid="claim-form-corrects"
            aria-label="Corrects claim number"
            placeholder="corrects no."
            value={corrects}
            onChange={(e) => {
              setCorrects(e.target.value);
              clearError();
            }}
          />{' '}
          <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
            {editing === null ? 'Add claim' : 'Save claim'}
          </button>
          {(invalid || (save.isError && refused === null)) && (
            <span data-testid="claim-form-error" role="alert">
              {' '}
              {invalid
                ? 'The amount must have up to two decimals; a negative amount must name the number of the settled claim it corrects.'
                : save.error instanceof HttpError && save.error.status === 409 && save.error.body.includes('retired')
                  ? `${person.name} is retired from ${person.retiredFrom}: a claim cannot be dated after it.`
                  : save.error instanceof HttpError && save.error.body.includes('correction')
                    ? `A correction must name a settled claim of ${person.name}.`
                  : 'Could not save this claim.'}
            </span>
          )}
        </form>
        <ul data-testid="claim-list">
          {claims.map((c) => (
            <li key={c.id} data-testid={`claim-row-${c.id}`}>
              <input
                type="checkbox"
                data-testid="claim-select"
                aria-label="Select claim"
                checked={selected.includes(c.id)}
                onChange={(e) =>
                  setSelected(e.target.checked ? [...selected, c.id] : selected.filter((id) => id !== c.id))
                }
              />{' '}
              <span data-testid="claim-amount">{c.amount}</span>{' '}
              <span data-testid="claim-date">{c.date}</span>{' '}
              <span data-testid="claim-period">{periodOf(periods, c.date)?.name ?? ''}</span>{' '}
              <span data-testid="claim-note">{c.note}</span>{' '}
              <span data-testid="claim-due">{showDue(claimDueCents(c, floor, rateOn(rates, gradeOn(person, c.date), c.date), lateBy))}</span>{' '}
              <span data-testid="claim-number">{c.number ?? ''}</span>{' '}
              <span data-testid="claim-corrects">{c.corrects ?? ''}</span>{' '}
              <span data-testid="claim-status">{c.status}</span>{' '}
              <span data-testid="claim-opened-by">{c.openedBy ?? ''}</span>{' '}
              <span data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-due-by">{c.dueBy}</span>{' '}
              <span data-testid="claim-overdue">{overdue(c, today) ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-deferred">{c.deferred ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-deferred-until">{c.deferredUntil ?? ''}</span>{' '}
              <span data-testid="claim-proofs">{c.proofs}</span>{' '}
              <span data-testid="claim-split-from">{c.splitFrom ?? ''}</span>{' '}
              <span data-testid="claim-late-entry">{c.lateEntry ? 'yes' : 'no'}</span>{' '}
              <button
                type="button"
                data-testid="claim-proof"
                disabled={addProof.isPending}
                onClick={() => addProof.mutate({ id: c.id })}
              >
                Add proof
              </button>{' '}
              {ACTIONS.map((action) => (
                <span key={action}>
                  {action === 'reject' && (
                    <>
                      <input
                        data-testid="claim-reject-reason"
                        aria-label="Reason for rejecting"
                        placeholder="reason"
                        value={reasons[c.id] ?? ''}
                        onChange={(e) => {
                          setReasons({ ...reasons, [c.id]: e.target.value });
                          if (refused?.id === c.id) setRefused(null);
                        }}
                      />{' '}
                    </>
                  )}
                  <button
                    type="button"
                    data-testid={`claim-${action}`}
                    disabled={changeStatus.isPending}
                    onClick={() => {
                      if (action === 'reject' && (reasons[c.id] ?? '').trim() === '') {
                        // Refused here, before anything is sent: the store is untouched.
                        setRefused({ id: c.id, message: NO_REASON });
                        return;
                      }
                      changeStatus.mutate({ id: c.id, action, reason: action === 'reject' ? (reasons[c.id] ?? '').trim() : undefined });
                    }}
                  >
                    {ACTION_LABELS[action]}
                  </button>{' '}
                </span>
              ))}
              <button
                type="button"
                data-testid="claim-resettle"
                disabled={resettle.isPending}
                onClick={() => resettle.mutate({ id: c.id })}
              >
                Re-settle
              </button>{' '}
              <input
                data-testid="claim-defer-date"
                aria-label="Defer until"
                placeholder="yyyy-mm-dd"
                value={deferTo[c.id] ?? ''}
                onChange={(e) => {
                  setDeferTo({ ...deferTo, [c.id]: e.target.value });
                  if (refused?.id === c.id) setRefused(null);
                }}
              />{' '}
              <button
                type="button"
                data-testid="claim-defer"
                disabled={defer.isPending}
                onClick={() => defer.mutate({ id: c.id, until: (deferTo[c.id] ?? '').trim() })}
              >
                Defer
              </button>{' '}
              <input
                data-testid="claim-date-new"
                aria-label="New date"
                placeholder="yyyy-mm-dd"
                value={redateTo[c.id] ?? ''}
                onChange={(e) => {
                  setRedateTo({ ...redateTo, [c.id]: e.target.value });
                  if (refused?.id === c.id) setRefused(null);
                }}
              />{' '}
              <button
                type="button"
                data-testid="claim-redate"
                disabled={redate.isPending}
                onClick={() => redate.mutate({ id: c.id, date: (redateTo[c.id] ?? '').trim() })}
              >
                Re-date
              </button>{' '}
              <input
                data-testid="claim-split-amount"
                aria-label="Amount to split off"
                placeholder="0.00"
                value={splitOff[c.id] ?? ''}
                onChange={(e) => {
                  setSplitOff({ ...splitOff, [c.id]: e.target.value });
                  if (refused?.id === c.id) setRefused(null);
                }}
              />{' '}
              <button
                type="button"
                data-testid="claim-split"
                disabled={split.isPending}
                onClick={() => {
                  const amount = (splitOff[c.id] ?? '').trim();
                  if (!/^\d+(\.\d{1,2})?$/.test(amount)) {
                    // Refused here, before anything is sent: the store is untouched.
                    setRefused({ id: c.id, message: BAD_SPLIT });
                    return;
                  }
                  split.mutate({ id: c.id, amount });
                }}
              >
                Split
              </button>{' '}
              <select
                data-testid="claim-reassign-to"
                aria-label="Reassign to"
                value={moveTo[c.id] ?? ''}
                onChange={(e) => {
                  setMoveTo({ ...moveTo, [c.id]: e.target.value });
                  if (refused?.id === c.id) setRefused(null);
                }}
              >
                <option value="">reassign to…</option>
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
                  const to = moveTo[c.id] ?? '';
                  if (to === '') {
                    // Refused here, before anything is sent: the store is untouched.
                    setRefused({ id: c.id, message: NO_TARGET });
                    return;
                  }
                  reassign.mutate({ id: c.id, toPersonId: Number(to) });
                }}
              >
                Reassign
              </button>{' '}
              <button type="button" data-testid="claim-edit" onClick={() => openEditor(c)}>
                Edit
              </button>
              {editing === c.id && (
                <>
                  {' '}
                  <button type="button" data-testid="claim-cancel" onClick={cancel}>
                    Cancel
                  </button>
                </>
              )}
              {refused?.id === c.id && (
                <span data-testid="claim-error" role="alert">
                  {' '}
                  {refused.message}
                </span>
              )}
              <ol data-testid="claim-log">
                {c.log.map((line, i) => (
                  <li key={i} data-testid="claim-log-line">
                    {line}
                  </li>
                ))}
              </ol>
            </li>
          ))}
        </ul>
      </td>
    </>
  );
}

export const contributions = [
  PersonColumns.fill({
    order: 10,
    Component: () => (
      <>
        <th>Claimed</th>
        <th>Due</th>
        <th>Excess</th>
        <th>Overdue</th>
        <th>Overdue due</th>
        <th>Claims</th>
      </>
    ),
  }),
  PersonCells.fill({ order: 10, Component: PersonClaims }),
];
