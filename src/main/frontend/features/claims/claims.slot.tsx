import { useEffect, useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { gradeOn, heldOn, usePeople, type Person } from '../people/usePeople';
import { periodOf, usePeriods } from '../periods/usePeriods';
import { claimRateOn, rateOn, showDue, useRates } from '../rates/useRates';
import { useCap, useFloor, useLate, useReviewLate } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheet';
import { useSelectedClaims } from '../bulk/useSelectedClaims';
import { useVoidReasons } from '../voidreasons/useVoidReasons';
import { useTemplates } from '../templates/useTemplates';
import { Col } from '../columns/useColumns';
import { ClaimTimeline } from './ClaimTimeline';
import {
  AMOUNT,
  CLAIM_NUMBER,
  claimDueCents,
  claimed,
  claimsKey,
  isOpen,
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
const NOT_REVIEWER = 'Only a steward can review a claim in REVIEW, which opens it.';
const NOT_STEWARD = 'Only a steward can approve a pending claim, and never the user who opened it.';
const NOT_ALLOWED = "Your role may not do that: the sheet's permission matrix decides who opens, settles and voids claims.";
const CANNOT_VOID_SETTLED = 'Only a steward can void a SETTLED claim, and only within 7 days of its settlement (never one due by in a closed period).';
const NO_REASON = 'A rejection needs a reason.';
const NO_VOID_REASON = "Choose a void reason from the sheet's list to void this claim.";
const NOT_STEWARD_MOVE = "Your role may not reassign a claim (by default only a steward can).";
const CANNOT_MOVE = 'Only a DRAFT, OPEN or PENDING claim can be reassigned to another person.';
const CANNOT_RESETTLE = 'Only a steward can re-settle a claim, and only a SETTLED one dated outside a closed period.';
const NO_TARGET = 'Choose another person to reassign this claim to.';
const CANNOT_DEFER = "Only an OPEN claim dated outside a closed period can be deferred, and only to a date after the sheet's today.";
const CANNOT_REDATE = "Only a steward can re-date a claim, only an OPEN or PENDING one, never one due by in a closed period, nor to a date after its person's retirement.";
const DEFERRED = 'A deferred claim cannot be settled until the sheet\'s today reaches its deferred-until date.';
const NEEDS_PROOF = "A claim whose due is above the proof threshold cannot be settled or approved until it has a proof (one of each of the sheet's proof kinds, when set).";
const NEEDS_NOTE = "A claim whose due is above the sheet's note threshold cannot be opened while its note is empty.";
const CANNOT_PROVE = 'A proof cannot be added to a SETTLED or VOID claim, nor to one due by in a closed period.';
const CANNOT_SPLIT = "Only an OPEN claim (not deferred, nor due by in a closed period) can be split, by a user whose role may open a claim.";
const BAD_SPLIT = "The amount split off must be more than 0.00 and less than the claim's amount, with up to two decimals.";
const NOT_DRAFT = 'Only a DRAFT claim can be edited: to change an open claim, void it and add a new one.';
const LOCKED = 'Another user is editing this claim: it is locked until they save or cancel their editor.';
const CLOSED_EDIT = 'This claim is due by in a closed period: it cannot be edited.';
const BAD_AMOUNT = 'The amount must have up to two decimals; a negative amount must name the number of the settled claim it corrects.';
const CANNOT_REMIND = "Only an OPEN or PENDING claim can be reminded, and only once per sheet's today.";
const CANNOT_WAIVE = "Only a steward can waive a claim's late charge, only on an OPEN or PENDING claim, and only once.";
const EMPTY_COMMENT = 'A comment cannot be empty.';
const BAD_LINE = "A line needs a label, a whole quantity of at least 1 and a unit amount with up to two decimals.";
const LINED = 'This claim has lines: its amount is them added up, and cannot be typed.';
const VIEWER = 'A viewer can change nothing.';
const CANNOT_DELETE = 'Only a DRAFT claim can be deleted, and never one due by in a closed period.';
const CANNOT_RESTORE = 'A deleted claim can be restored to DRAFT only on the sheet\'s today it was deleted.';
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

const ACTIONS = ['open', 'review', 'settle', 'approve', 'reject', 'void', 'delete', 'restore'] as const;
type Action = (typeof ACTIONS)[number];
const ACTION_LABELS: Record<Action, string> = { open: 'Open', review: 'Review', settle: 'Settle', approve: 'Approve', reject: 'Reject', void: 'Void', delete: 'Delete', restore: 'Restore' };

/**
 * A person's claims: what they have claimed in all and their due-total, the sum of their claims' rounded dues,
 * one form to add a claim or edit the one whose editor is open (only a DRAFT outside a closed period can be edited; an open
 * editor locks its claim for every other user until it is saved or cancelled, or the sheet's today changes), and the claims, newest first, each
 * with its period (the one whose dates contain its due-by, if any), its due (fixed once it is settled, else bound by the last statement, else at the rate in force on the claim's date, plus its late charge, at least the sheet's floor; in the
 * due-total too), its status, who opened it, whether a statement binds it and the buttons to open, settle, approve, reject (with a reason) or void it (a SETTLED one only by a steward within 7 days of its settlement; its opener or last editor cannot settle
 * or approve it), and for a steward to re-settle a SETTLED one at today's figure. A clerk's settle over the clerk limit leaves the claim PENDING until a steward approves
 * or rejects it; while PENDING it shows its live figure, not the one a statement bound it to.
 * Only OPEN, PENDING and SETTLED claims count toward claimed and the due-total, and not the OPEN or
 * PENDING ones dated within one of the person's holds, unless a statement binds them; claimed leaves out
 * claims that correct another.
 * A claim with a negative amount is a correction: it names, by number, a SETTLED claim of the person
 * that it corrects (refused otherwise), shows that number, and its due has no floor and no late charge.
 * Each claim shows its due-by (fixed when it was added: the terms-th business day after its date, from the terms of its person's grade) and whether it is overdue (OPEN and
 * due by before the sheet's today); the person shows how many of the claims that count are overdue
 * and their dues added up.
 * A steward can reassign a DRAFT, OPEN or PENDING claim to another person on the sheet: it keeps its
 * number and date, is no longer bound by a statement, and is listed as its new person's newest claim.
 * A steward can re-date an OPEN or PENDING claim (not one due by in a closed period, nor after its person's
 * retirement): its due-by and any figure a statement bound stay, and so do its period and number.
 * An OPEN claim can be deferred until a date after the sheet's today: until today reaches it, it shows
 * deferred yes and its deferred-until date, counts in no total, is not overdue and cannot be settled.
 * An OPEN or PENDING claim can be reminded once per sheet's today: it shows that day as its reminded date.
 * A steward can waive an OPEN or PENDING claim's late charge, once: it shows waived yes and bears no late
 * charge from then on, whatever its overdue state.
 * Each claim shows its proofs count, raised by one with its proof button (not on a SETTLED or VOID claim,
 * nor one due by in a closed period); one whose due is above the proof threshold on its date (its person's grade's, else the sheet's)
 * cannot be settled or approved while it has none. A proof's kind, receipt or photo, is chosen when it is added
 * (a receipt when none is); the claim shows its count of each beside the total, and while the sheet's proof kinds
 * are set, a claim above the proof threshold needs a proof of each listed kind before it can be settled or approved.
 * One whose due is above the sheet's note threshold on its date cannot be opened, alone or in bulk,
 * while its note is empty.
 * An OPEN claim can be split: an amount (more than 0.00, less than its amount) goes off it into a new
 * OPEN claim with the same date and note, the next number, opened by the user, which shows the
 * original's number as split-from; the original keeps the rest and is no longer bound by a statement.
 * A DRAFT claim (not one due by in a closed period) can be deleted: it shows DELETED, counts nowhere and
 * cannot be edited, opened or voided; on the sheet's today it was deleted it can be restored to DRAFT.
 * A claim can be commented: each comment shows on it, oldest first, as "<user> on <date>: <text>"
 * (the sheet's today when it was written); an empty comment is refused.
 * A DRAFT claim can have lines, each shown as "<label> <qty> x <unit>": adding one edits the claim (refused
 * as an edit is), and its amount becomes the quantities times the unit amounts added up; an edit that
 * types another amount is then refused.
 */
function PersonClaims({ user, sheet, person }: { user: string; sheet: string; person: Person }) {
  const queryClient = useQueryClient();
  const { data: all = [] } = useClaims(sheet);
  const claims = all.filter((c) => c.personId === person.id);
  const today = useSheetToday(sheet);
  const reviewLate = useReviewLate(sheet);
  // Their OPEN and PENDING claims dated within a hold, or all of them once today reaches their
  // retirement date, count toward neither claimed nor the due-total, unless a statement binds them.
  const liveClaims = live(claims, (_, date) => heldOn(person, date, today));
  const { data: everyone = [] } = usePeople(sheet);
  const others = everyone.filter((p) => p.id !== person.id);
  const { data: rates = [] } = useRates(sheet);
  const { data: periods = [] } = usePeriods(sheet);
  const pct = rateOn(rates, gradeOn(person, today), today);
  const floor = useFloor(sheet);
  const lateBy = useLate(sheet);
  const cap = useCap(sheet);
  // The sum of their figures; their due-total is at most the cap, the rest their excess.
  const dueSum = personDueCents(liveClaims, floor, pct, (c) => claimRateOn(rates, gradeOn(person, c.date), c), lateBy);
  // The due-total before the cap, split (an n/a part shows 0.00): their SETTLED claims' figures, and their OPEN and PENDING ones'.
  const settledSum = personDueCents(liveClaims.filter((c) => c.status === 'SETTLED'), floor, pct, (c) => claimRateOn(rates, gradeOn(person, c.date), c), lateBy);
  const openSum = personDueCents(liveClaims.filter(isOpen), floor, pct, (c) => claimRateOn(rates, gradeOn(person, c.date), c), lateBy);
  const late = overdueFigures(liveClaims, lateBy, floor, (c) => claimRateOn(rates, gradeOn(person, c.date), c));
  // The claim whose editor this user has open: the one their lock holds, so it stays open across a re-load.
  const editingClaim = claims.find((c) => c.lockedBy === user) ?? null;
  const editing = editingClaim?.id ?? null;
  const [amount, setAmount] = useState('');
  const [date, setDate] = useState('');
  const [note, setNote] = useState('');
  const [corrects, setCorrects] = useState('');
  const [ref, setRef] = useState('');
  const [tags, setTags] = useState('');
  const [invalid, setInvalid] = useState(false);
  // The claim template chosen to add a claim from.
  const { data: templates = [] } = useTemplates(sheet);
  const [templateName, setTemplateName] = useState('');
  // The claim whose last status change or edit was refused: its row shows the error.
  const [refused, setRefused] = useState<{ id: number; message: string } | null>(null);
  // The claims ticked for a bulk action (the bulk-open panel acts on them).
  const [selected, setSelected] = useSelectedClaims();
  // The reason typed for rejecting each pending claim, by claim id.
  const [reasons, setReasons] = useState<Record<number, string>>({});
  // The void reason chosen for each claim, by claim id: one is needed while the sheet has any.
  const { data: voidReasons = [] } = useVoidReasons(sheet);
  const [voidWith, setVoidWith] = useState<Record<number, string>>({});
  // The date typed for deferring each claim, by claim id.
  const [deferTo, setDeferTo] = useState<Record<number, string>>({});
  // The new date typed for re-dating each claim, by claim id.
  const [redateTo, setRedateTo] = useState<Record<number, string>>({});
  // The kind chosen for each claim's next proof ('' for none: a receipt).
  const [proofKind, setProofKind] = useState<Record<number, string>>({});
  // The person chosen to reassign each claim to, by claim id.
  const [moveTo, setMoveTo] = useState<Record<number, string>>({});
  // The amount typed for splitting each claim, by claim id.
  const [splitOff, setSplitOff] = useState<Record<number, string>>({});
  // The comment typed for each claim, by claim id.
  const [commentText, setCommentText] = useState<Record<number, string>>({});
  // The line typed for each claim, by claim id.
  const NO_LINE = { label: '', qty: '', unit: '' };
  const [lineTyped, setLineTyped] = useState<Record<number, { label: string; qty: string; unit: string }>>({});
  // The amount typed in each claim's row for its inline edit, by claim id.
  const [inlineAmount, setInlineAmount] = useState<Record<number, string>>({});
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
    mutationFn: async (c: { id: number | null; amount: string; date: string; note: string; corrects: string; ref: string; tags: string }) => {
      // An edit goes only once its editor's lock is taken; a refused lock is shown on the claim.
      const refusal = c.id !== null ? await locking.current : null;
      if (refusal !== null && refusal.id === c.id) throw new LockRefused(refusal);
      const body = { amount: c.amount, date: c.date, note: c.note, corrects: c.corrects, ref: c.ref, tags: c.tags, user };
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
      if (c.action === 'void') setVoidWith((r) => ({ ...r, [c.id]: '' }));
    },
    onError: (e, c) =>
      setRefused({
        id: c.id,
        message: c.action === 'delete'
          ? CANNOT_DELETE
          : c.action === 'restore'
            ? CANNOT_RESTORE
          : !(e instanceof HttpError)
          ? STATUS_REFUSED
          : e.body.includes('void reason')
            ? NO_VOID_REASON
          : e.body.includes('void a settled')
            ? CANNOT_VOID_SETTLED
          : e.body.includes('proof threshold')
            ? NEEDS_PROOF
          : e.body.includes('note threshold')
            ? NEEDS_NOTE
          : e.body.includes('clerk limit')
            ? OVER_LIMIT
            : e.body.includes('deferred')
              ? DEFERRED
            : e.body.includes('Your role')
              ? NOT_ALLOWED
            : e.body.includes('approve')
              ? NOT_STEWARD
            : c.action === 'review'
              ? NOT_REVIEWER
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

  // An OPEN or PENDING claim is reminded, once per sheet's today.
  const remind = useMutation({
    mutationFn: (c: { id: number }) => postJson<Claim>(`${base}/claims/${c.id}/remind`, { user }),
    onMutate: () => setRefused(null),
    onError: (e, c) =>
      setRefused({
        id: c.id,
        message: e instanceof HttpError && e.body.includes('viewer')
          ? VIEWER
          : e instanceof HttpError && e.body.includes('archived')
            ? ARCHIVED
            : CANNOT_REMIND,
      }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });

  // A steward waives an OPEN or PENDING claim's late charge, once.
  const waive = useMutation({
    mutationFn: (c: { id: number }) => postJson<Claim>(`${base}/claims/${c.id}/waive`, { user }),
    onMutate: () => setRefused(null),
    onError: (_, c) => setRefused({ id: c.id, message: CANNOT_WAIVE }),
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

  // A proof raises the claim's proofs count by one, of the kind chosen (a receipt when none is).
  const addProof = useMutation({
    mutationFn: (c: { id: number; kind: string }) =>
      postJson<Claim>(`${base}/claims/${c.id}/proof`, c.kind === '' ? { user } : { user, kind: c.kind }),
    onMutate: () => setRefused(null),
    onSuccess: (_, c) => setProofKind((k) => ({ ...k, [c.id]: '' })),
    onError: (_, c) => setRefused({ id: c.id, message: CANNOT_PROVE }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });

  // A comment kept on the claim with who wrote it and the sheet's today.
  const addComment = useMutation({
    mutationFn: (c: { id: number; text: string }) => {
      if (c.text.trim() === '') return Promise.reject(new Error(EMPTY_COMMENT));
      return postJson<Claim>(`${base}/claims/${c.id}/comments`, { text: c.text, user });
    },
    onMutate: () => setRefused(null),
    onSuccess: (_, c) => setCommentText((t) => ({ ...t, [c.id]: '' })),
    onError: (e, c) =>
      setRefused({
        id: c.id,
        message: e instanceof HttpError && e.body.includes('viewer')
          ? VIEWER
          : e instanceof HttpError && e.body.includes('archived')
            ? ARCHIVED
            : e instanceof HttpError && e.message.includes('as of a date')
              ? 'The sheet is shown as of a date: nothing can be changed.'
              : e instanceof HttpError && !e.body.includes('empty')
                ? 'Could not comment on this claim.'
                : EMPTY_COMMENT,
      }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });

  // A line on a DRAFT claim, as an edit of it: the claim's amount becomes its lines added up.
  const addLine = useMutation({
    mutationFn: (c: { id: number; label: string; qty: string; unit: string }) => {
      if (c.label.trim() === '' || !/^\d+$/.test(c.qty.trim()) || Number(c.qty.trim()) < 1
        || !/^\d+(\.\d{1,2})?$/.test(c.unit.trim())) {
        return Promise.reject(new Error(BAD_LINE));
      }
      return postJson<Claim>(`${base}/claims/${c.id}/lines`, { label: c.label, qty: c.qty, unit: c.unit, user });
    },
    onMutate: () => setRefused(null),
    onSuccess: (_, c) => setLineTyped((t) => ({ ...t, [c.id]: NO_LINE })),
    onError: (e, c) =>
      setRefused({
        id: c.id,
        message: !(e instanceof HttpError)
          ? BAD_LINE
          : e.body.includes('locked')
            ? LOCKED
            : e.body.includes('viewer')
              ? VIEWER
              : e.body.includes('archived')
                ? ARCHIVED
                : e.body.includes('closed')
                  ? CLOSED_EDIT
                  : e.body.includes('DRAFT')
                    ? NOT_DRAFT
                    : e.status === 400
                      ? BAD_LINE
                      : 'Could not add a line to this claim.',
      }),
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

  // A DRAFT claim's amount changed in its row: the claim form's edit, with the claim's other values as they are.
  const editAmount = useMutation({
    mutationFn: (c: { claim: Claim; amount: string }) =>
      putJson<Claim>(`${base}/claims/${c.claim.id}`, {
        amount: c.amount,
        date: c.claim.date,
        note: c.claim.note,
        corrects: c.claim.corrects ?? '',
        ref: c.claim.ref ?? '',
        tags: c.claim.tags.join(','),
        user,
      }),
    onMutate: () => setRefused(null),
    onSuccess: (_, c) => setInlineAmount((a) => ({ ...a, [c.claim.id]: '' })),
    onError: (e, c) =>
      setRefused({
        id: c.claim.id,
        message: !(e instanceof HttpError)
          ? 'Could not save this claim.'
          : e.body.includes('locked')
            ? LOCKED
            : e.body.includes('viewer')
              ? VIEWER
              : e.body.includes('archived')
                ? ARCHIVED
                : e.body.includes('closed')
                  ? CLOSED_EDIT
                  : e.body.includes('DRAFT')
                    ? NOT_DRAFT
                    : e.body.includes('retired')
                      ? e.body
                      : e.body.includes('lines')
                        ? LINED
                      : e.status === 400 || e.body.includes('correction')
                        ? BAD_AMOUNT
                        : 'Could not save this claim.',
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
              ? 'This claim is due by in a closed period.'
              : CANNOT_MOVE,
      }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });

  // The edit button: the server locks the claim for this user, or refuses (another user's lock, not a
  // DRAFT, due by in a closed period, a viewer) on the claim itself, taking no lock.
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

  // A DRAFT claim from a template as the server holds it now, by every rule of the claim form.
  const fromTemplate = useMutation({
    mutationFn: (name: string) => postJson<Claim>(`${base}/people/${person.id}/template-claims`, { template: name, user }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
  });

  // An error goes as soon as its form is changed or submitted again, or an editor is opened.
  const clearError = () => {
    save.reset();
    fromTemplate.reset();
    setInvalid(false);
    setRefused(null);
  };
  const close = () => {
    setAmount('');
    setDate('');
    setNote('');
    setCorrects('');
    setRef('');
    setTags('');
  };
  const fill = (c: Claim) => {
    setAmount(c.amount);
    setDate(c.date);
    setNote(c.note);
    setCorrects(c.corrects === null ? '' : String(c.corrects));
    setRef(c.ref ?? '');
    setTags(c.tags.join(', '));
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

  const claimError = fromTemplate.error ?? save.error;

  return (
    <>
      <Col user={user} id="claimed"><td data-testid="person-claimed">{claimed(liveClaims)}</td></Col>
      <Col user={user} id="due-total"><td data-testid="person-due-total">{showDue(capCents(dueSum, cap))}</td></Col>
      <Col user={user} id="excess"><td data-testid="person-excess">{showDue(excessCents(dueSum, cap))}</td></Col>
      <td data-testid="person-settled-total">{showDue(settledSum ?? 0n)}</td>
      <td data-testid="person-open-total">{showDue(openSum ?? 0n)}</td>
      <Col user={user} id="overdue-count"><td data-testid="person-overdue-count">{late.count}</td></Col>
      <Col user={user} id="overdue-total"><td data-testid="person-overdue-total">{showDue(late.cents)}</td></Col>
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
            submitOnce(() => save.mutateAsync({ id: editing, amount: amount.trim(), date: date.trim(), note, corrects: corrects.trim(), ref: ref.trim(), tags }));
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
          <input
            data-testid="claim-form-ref"
            aria-label="External reference"
            placeholder="ref"
            value={ref}
            onChange={(e) => {
              setRef(e.target.value);
              clearError();
            }}
          />{' '}
          <input
            data-testid="claim-form-tags"
            aria-label="Tags, comma separated"
            placeholder="tags"
            value={tags}
            onChange={(e) => {
              setTags(e.target.value);
              clearError();
            }}
          />{' '}
          <button type="submit" data-testid="claim-form-submit" disabled={save.isPending}>
            {editing === null ? 'Add claim' : 'Save claim'}
          </button>
          {(invalid || (save.isError && refused === null) || fromTemplate.isError) && (
            <span data-testid="claim-form-error" role="alert">
              {' '}
              {invalid
                ? BAD_AMOUNT
                : claimError instanceof HttpError && claimError.status === 409 && claimError.body.includes('retired')
                  ? claimError.body
                  : claimError instanceof HttpError && claimError.body.includes('correction')
                    ? `A correction must name a settled claim of ${person.name}.`
                  : claimError instanceof HttpError && claimError.body.includes('lines')
                    ? LINED
                  : claimError instanceof HttpError && claimError.body.includes('reference')
                    ? `The reference ${ref.trim()} is already used on this sheet.`
                  : 'Could not save this claim.'}
            </span>
          )}
        </form>
        <select
          data-testid="claim-template-select"
          aria-label="Claim template"
          value={templateName}
          onChange={(e) => {
            setTemplateName(e.target.value);
            clearError();
          }}
        >
          <option value="">template…</option>
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
            clearError();
            if (templateName === '') {
              // Nothing chosen: refused here, before anything is sent.
              setInvalid(true);
              return;
            }
            submitOnce(() => fromTemplate.mutateAsync(templateName));
          }}
        >
          Add from template
        </button>
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
              <span data-testid="claim-period">{periodOf(periods, c.dueBy)?.name ?? ''}</span>{' '}
              <span data-testid="claim-note">{c.note}</span>{' '}
              <span data-testid="claim-due">{showDue(claimDueCents(c, floor, claimRateOn(rates, gradeOn(person, c.date), c), lateBy))}</span>{' '}
              <span data-testid="claim-number">{c.number ?? ''}</span>{' '}
              <span data-testid="claim-corrects">{c.corrects ?? ''}</span>{' '}
              <span data-testid="claim-status">{c.status}</span>{' '}
              <span data-testid="claim-review-late">{reviewLate(c) ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-batch">{c.batch ?? ''}</span>{' '}
              <span data-testid="claim-opened-by">{c.openedBy ?? ''}</span>{' '}
              <span data-testid="claim-stated">{c.stated ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-due-by">{c.dueBy}</span>{' '}
              <span data-testid="claim-overdue">{overdue(c, lateBy) ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-deferred">{c.deferred ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-deferred-until">{c.deferredUntil ?? ''}</span>{' '}
              <span data-testid="claim-proofs">{c.proofs}</span>{' '}
              (receipts <span data-testid="claim-proofs-receipt">{c.proofReceipts}</span>, photos{' '}
              <span data-testid="claim-proofs-photo">{c.proofPhotos}</span>){' '}
              <span data-testid="claim-split-from">{c.splitFrom ?? ''}</span>{' '}
              <span data-testid="claim-late-entry">{c.lateEntry ? 'yes' : 'no'}</span>{' '}
              <span data-testid="claim-reminded">{c.remindedOn ?? ''}</span>{' '}
              <span data-testid="claim-waived">{c.waived ? 'yes' : 'no'}</span>{' '}
              <button
                type="button"
                data-testid="claim-waive"
                disabled={waive.isPending}
                onClick={() => waive.mutate({ id: c.id })}
              >
                Waive late charge
              </button>{' '}
              <span data-testid="claim-ref">{c.ref ?? ''}</span>{' '}
              <span data-testid="claim-tags">{c.tags.join(', ')}</span>{' '}
              <button
                type="button"
                data-testid="claim-remind"
                disabled={remind.isPending}
                onClick={() => remind.mutate({ id: c.id })}
              >
                Remind
              </button>{' '}
              <select
                data-testid="claim-proof-kind"
                aria-label="Proof kind"
                value={proofKind[c.id] ?? ''}
                onChange={(e) => {
                  setProofKind({ ...proofKind, [c.id]: e.target.value });
                  if (refused?.id === c.id) setRefused(null);
                }}
              >
                <option value="">(kind)</option>
                <option value="receipt">receipt</option>
                <option value="photo">photo</option>
              </select>{' '}
              <button
                type="button"
                data-testid="claim-proof"
                disabled={addProof.isPending}
                onClick={() => addProof.mutate({ id: c.id, kind: proofKind[c.id] ?? '' })}
              >
                Add proof
              </button>{' '}
              <input
                data-testid="claim-comment-text"
                aria-label="Comment"
                value={commentText[c.id] ?? ''}
                onChange={(e) => {
                  if (refused?.id === c.id) setRefused(null);
                  setCommentText((t) => ({ ...t, [c.id]: e.target.value }));
                }}
              />{' '}
              <button
                type="button"
                data-testid="claim-comment-add"
                disabled={addComment.isPending}
                onClick={() => addComment.mutate({ id: c.id, text: commentText[c.id] ?? '' })}
              >
                Comment
              </button>{' '}
              {(['label', 'qty', 'unit'] as const).map((k) => (
                <span key={k}>
                  <input
                    data-testid={`claim-line-${k}`}
                    aria-label={k === 'label' ? 'Line label' : k === 'qty' ? 'Line quantity' : 'Line unit amount'}
                    placeholder={k === 'label' ? 'label' : k === 'qty' ? '1' : '0.00'}
                    size={k === 'label' ? undefined : 5}
                    value={lineTyped[c.id]?.[k] ?? ''}
                    onChange={(e) => {
                      if (refused?.id === c.id) setRefused(null);
                      const v = e.target.value;
                      setLineTyped((t) => ({ ...t, [c.id]: { ...(t[c.id] ?? NO_LINE), [k]: v } }));
                    }}
                  />{' '}
                </span>
              ))}
              <button
                type="button"
                data-testid="claim-line-add"
                disabled={addLine.isPending}
                onClick={() => addLine.mutate({ id: c.id, ...(lineTyped[c.id] ?? NO_LINE) })}
              >
                Add line
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
                  {action === 'void' && voidReasons.length > 0 && (
                    <>
                      <select
                        data-testid="claim-void-reason"
                        aria-label="Reason for voiding"
                        value={voidWith[c.id] ?? ''}
                        onChange={(e) => {
                          setVoidWith({ ...voidWith, [c.id]: e.target.value });
                          if (refused?.id === c.id) setRefused(null);
                        }}
                      >
                        <option value="">(void reason)</option>
                        {voidReasons.map((v) => (
                          <option key={v.reason} value={v.reason}>
                            {v.reason}
                          </option>
                        ))}
                      </select>{' '}
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
                      const voidReason = voidWith[c.id] ?? '';
                      if (action === 'void' && voidReasons.length > 0 && !voidReasons.some((v) => v.reason === voidReason)) {
                        // Refused here, before anything is sent: the store is untouched.
                        setRefused({ id: c.id, message: NO_VOID_REASON });
                        return;
                      }
                      changeStatus.mutate({
                        id: c.id,
                        action,
                        reason:
                          action === 'reject'
                            ? (reasons[c.id] ?? '').trim()
                            : action === 'void' && voidReason !== ''
                              ? voidReason
                              : undefined,
                      });
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
              <input
                data-testid="claim-inline-amount"
                aria-label="New amount"
                placeholder="0.00"
                value={inlineAmount[c.id] ?? ''}
                onChange={(e) => {
                  setInlineAmount({ ...inlineAmount, [c.id]: e.target.value });
                  if (refused?.id === c.id) setRefused(null);
                }}
              />{' '}
              <button
                type="button"
                data-testid="claim-inline-save"
                disabled={editAmount.isPending}
                onClick={() => {
                  const amount = (inlineAmount[c.id] ?? '').trim();
                  // The claim form's rule: a negative amount is a correction, naming the claim it corrects.
                  if (!AMOUNT.test(amount) || (amount.startsWith('-') && !CLAIM_NUMBER.test(c.corrects ?? ''))) {
                    // Refused here, before anything is sent: the store is untouched.
                    setRefused({ id: c.id, message: BAD_AMOUNT });
                    return;
                  }
                  editAmount.mutate({ claim: c, amount });
                }}
              >
                Save amount
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
              {' '}
              <ClaimTimeline sheet={sheet} claimId={c.id} />
              <ol data-testid="claim-log">
                {c.log.map((line, i) => (
                  <li key={i} data-testid="claim-log-line">
                    {line}
                  </li>
                ))}
              </ol>
              <ol data-testid="claim-comments">
                {c.comments.map((line, i) => (
                  <li key={i} data-testid="claim-comments-line">
                    {line}
                  </li>
                ))}
              </ol>
              <ol data-testid="claim-lines">
                {c.lines.map((line, i) => (
                  <li key={i} data-testid="claim-lines-line">
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
    Component: ({ user }: { user: string; sheet: string }) => (
      <>
        <Col user={user} id="claimed"><th>Claimed</th></Col>
        <Col user={user} id="due-total"><th>Due</th></Col>
        <Col user={user} id="excess"><th>Excess</th></Col>
        <th>Settled</th>
        <th>Open</th>
        <Col user={user} id="overdue-count"><th>Overdue</th></Col>
        <Col user={user} id="overdue-total"><th>Overdue due</th></Col>
        <th>Claims</th>
      </>
    ),
  }),
  PersonCells.fill({ order: 10, Component: PersonClaims }),
];
