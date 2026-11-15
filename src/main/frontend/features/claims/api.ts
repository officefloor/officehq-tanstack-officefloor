import { getJson, postJson, putJson } from '../../api/http';
import { sheetKey } from '../sheet/api';

/** A claim's status: DRAFT, opened to OPEN, settled to SETTLED (or, by a clerk above the clerk limit,
 *  to PENDING, which a steward approves to SETTLED or rejects back to OPEN); a DRAFT, OPEN or PENDING
 *  claim can be voided, and a SETTLED one by a steward within 7 days of its settlement. A DRAFT can be
 *  deleted to DELETED (counted nowhere), and restored to DRAFT on the sheet's today it was deleted. A DRAFT
 *  whose due is above the review threshold in force on its date opens to REVIEW instead (counted nowhere,
 *  unnumbered), which a steward reviews to OPEN; a REVIEW claim can be voided. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID' | 'DELETED' | 'REVIEW';
/** A change of a claim's status. */
export type ClaimAction = 'open' | 'settle' | 'void' | 'approve' | 'reject' | 'resettle' | 'delete' | 'restore' | 'review';

/** Whether a claim is open: OPEN, or PENDING a steward's approval. */
export const isOpen = (c: Pick<Claim, 'status'>) => c.status === 'OPEN' || c.status === 'PENDING';

/** Whether a statement's figure binds a claim now: it was bound to one (a PENDING claim too). */
export const isBound = (c: Claim) => c.stated && c.statedDue !== null;

/** A claim: its amount as two-decimal text, its date (yyyy-MM-dd), its note, its status and its
 *  number on the sheet (given when opened: the sheet-wide 1, 2, ... or, under period numbering,
 *  <period>-<n> or X-<n>; null while a draft), and its settled due
 *  (fixed when settled; null otherwise, or when there was no rate then — its due stays live), and who
 *  opened it (null while a draft; that user cannot settle it), and whether its person's last statement
 *  bound it, with the figure it bound (null when it had none): a bound claim shows and totals that.
 *  A correction (a negative amount) has the number of the SETTLED claim it corrects (null otherwise).
 *  `roundDate` is the date its live figures round by: its own, or a correction's claim's it corrects.
 *  `dueBy` (yyyy-MM-dd) is fixed when it was added: its date then plus the terms in force on the
 *  sheet's today then (the person's own, else their grade's that day, else the sheet's). `log` is its status log, oldest
 *  first: `<STATUS> by <user> on <date> due=<due>`, the sheet's today and its due at each change.
 *  `deferredUntil` is the date it was last deferred until (null: never), and `deferred` whether it is
 *  deferred on the sheet's today (OPEN, or VOID voided while deferred, and deferred until a later date): a deferred claim counts in no
 *  total, is not overdue, cannot be settled and is left unbound by a statement. `proofs` is how many
 *  proofs it has, `photoProofs` how many of them are photos (the rest are receipts): one whose due is
 *  above the proof threshold in force on its date cannot be settled or approved with none (while the
 *  sheet's proof kinds are set: with none of one of them). */
export type Claim = {
  id: number; sheetId: string; personId: number; amount: string; date: string; note: string; status: ClaimStatus;
  number: string | null; settledDue: string | null; openedBy: string | null;
  stated: boolean; statedDue: string | null; corrects: string | null; roundDate: string; dueBy: string;
  log: string[]; deferredUntil: string | null; deferred: boolean; proofs: number; photoProofs: number;
  /** Its comments, oldest first: `<user> on <date>: <text>` (the sheet's today when written). */
  comments: string[];
  /** Its lines, in the order they were added: `<label> <qty> x <unit>`; with lines, its amount is their total. */
  lines: string[];
  /** The number of the claim it was split from (null: not split off a claim). */
  splitFrom: string | null;
  /** The user whose open editor locks it for everyone else (null: unlocked). */
  lockedBy: string | null;
  /** Whether it was added while the period its due-by falls in was soft-closed. */
  lateEntry: boolean;
  /** The last sheet's today it was reminded on (null: never reminded). */
  reminded: string | null;
  /** Whether a steward waived its late charge: it bears none from then on, whatever its overdue state. */
  waived: boolean;
  /** Whether it is held: shown only; a held claim counts in every total as any other. */
  held: boolean;
  /** Its external reference (null: none); used once per sheet, compared without regard to case. */
  ref: string | null;
  /** Its tags: trimmed, lower-cased, without repeats, sorted and comma separated (null: none). */
  tags: string | null;
  /** The reason it was voided with, from the sheet's void reasons (null: none). */
  voidReason: string | null;
  /** The number of the bulk settle batch that settled it (null: not settled by a bulk settle). */
  settleBatch: number | null;
};
/** `corrects`: the number of the SETTLED claim of the same person a correction (a negative amount) corrects.
 *  `ref`: its external reference (trimmed; blank: none), refused when already used on the sheet.
 *  `tags`: its tags, comma separated (each trimmed and lower-cased, repeats dropped; blank: none). */
export type ClaimInput = { amount: string; date: string; note: string; corrects: string; ref: string; tags: string };

/** A claim's tags as shown: sorted, comma and space separated (blank: none). */
export const showTags = (tags: string | null) => (tags ?? '').split(',').filter((t) => t !== '').join(', ');

/** A claim's tags as a list (empty: none). */
export const tagsOf = (c: Pick<Claim, 'tags'>) => (c.tags ?? '').split(',').filter((t) => t !== '');

/** Whether a claim corrects another (names the claim it corrects): left out of claimed. */
export const correctsAnother = (c: Pick<Claim, 'corrects'>) => !!c.corrects?.trim();

/** Whether a claim is a correction: its amount is negative. */
export const isCorrection = (c: Pick<Claim, 'amount'>) => c.amount.trim().startsWith('-') && Number(c.amount) !== 0;

const sheetBase = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}`;

export const claimsKey = (sheetId: string) => [...sheetKey(sheetId), 'claims'] as const;

/** Under the claims' key, so whatever refreshes the claims refreshes the tags too. */
export const tagsKey = (sheetId: string) => [...claimsKey(sheetId), 'tags'] as const;

/** Every tag ever used on the sheet's claims, sorted. */
export function listTags(sheetId: string): Promise<string[]> {
  return getJson<string[]>(`${sheetBase(sheetId)}/tags`);
}

/** Every claim on the sheet, newest first. */
export function listClaims(sheetId: string, live = false): Promise<Claim[]> {
  return getJson<Claim[]>(`${sheetBase(sheetId)}/claims`, live);
}

/** Add a claim to a person; refused (rejects) when the amount is missing or has more than two
 *  decimals, the date is not a date, or it is a correction (negative) that does not name by number a
 *  SETTLED claim of the person, or its reference is already used on the sheet (in any case). A blank date takes the sheet's entry date when set, else its today. */
export function addClaim(sheetId: string, by: string, personId: number, input: ClaimInput): Promise<Claim> {
  return postJson<Claim>(`${sheetBase(sheetId)}/people/${personId}/claims?by=${encodeURIComponent(by)}`, input);
}

/** Edit a claim; refused (rejects) like an add. */
export function updateClaim(sheetId: string, by: string, id: number, input: ClaimInput): Promise<Claim> {
  return putJson<Claim>(`${sheetBase(sheetId)}/claims/${id}?by=${encodeURIComponent(by)}`, input);
}

/** Open a claim's editor, locking it for other users until it is saved or cancelled or the sheet's
 *  today changes (and releasing any other claim of its person the user held); refused (rejects) with
 *  409 when it is not a DRAFT, 423 when due by a day in a closed period, 412 while another user holds it. */
export function lockClaim(sheetId: string, by: string, id: number): Promise<Claim> {
  return postJson<Claim>(`${sheetBase(sheetId)}/claims/${id}/lock?by=${encodeURIComponent(by)}`, {});
}

/** Cancel a claim's editor, releasing its lock when the user holds it. */
export function unlockClaim(sheetId: string, by: string, id: number): Promise<Claim> {
  return postJson<Claim>(`${sheetBase(sheetId)}/claims/${id}/unlock?by=${encodeURIComponent(by)}`, {});
}

/** Change a claim's status; refused (rejects) when its status does not allow the change, when the
 *  user settling or approving it is the one who opened it, when a non-steward approves or rejects it,
 *  or when a rejection has no `reason`, or a void none from the sheet's void reasons while it has any. A steward's `resettle` freezes a SETTLED claim again at the
 *  figure it would have today as an open claim bound by no statement. `delete` makes a DRAFT DELETED (refused for
 *  any other status or one due by a day in a closed period); `restore` makes a DELETED claim a DRAFT again,
 *  refused on a later sheet's today than the one it was deleted on. A `settle` with a date `on` (blank: the
 *  sheet's today) freezes the due as it stood at the end of that day; refused for a date after the sheet's
 *  today or a claim that did not exist then. */
export function moveClaim(sheetId: string, by: string, id: number, action: ClaimAction, reason = '', on = ''): Promise<Claim> {
  const extra = action === 'reject' || action === 'void' ? `&reason=${encodeURIComponent(reason)}`
    : action === 'settle' && on !== '' ? `&on=${encodeURIComponent(on)}` : '';
  return postJson<Claim>(`${sheetBase(sheetId)}/claims/${id}/${action}?by=${encodeURIComponent(by)}${extra}`, {});
}

/** Defer an OPEN claim until a date after the sheet's today; refused (rejects) when it is not OPEN,
 *  the date is not after today, or it is due by a day in a closed period. */
export function deferClaim(sheetId: string, by: string, id: number, until: string): Promise<Claim> {
  return postJson<Claim>(`${sheetBase(sheetId)}/claims/${id}/defer?by=${encodeURIComponent(by)}&until=${encodeURIComponent(until)}`, {});
}

/** A claim's timeline: the audit lines about it exactly as the file holds them, newest first — those
 *  carrying its number, or for a claim with no number those naming its person, amount and date together. */
export function claimTimeline(sheetId: string, id: number): Promise<string[]> {
  return getJson<string[]>(`${sheetBase(sheetId)}/claims/${id}/timeline`);
}

/** A steward waives an OPEN or PENDING claim's late charge; refused (rejects) with 409 for anyone else,
 *  any other status, or a claim already waived. */
export function waiveClaim(sheetId: string, by: string, id: number): Promise<Claim> {
  return postJson<Claim>(`${sheetBase(sheetId)}/claims/${id}/waive?by=${encodeURIComponent(by)}`, {});
}

/** Hold an OPEN or PENDING claim, or release it when held; refused (rejects) with 409 for any other status. */
export function holdClaim(sheetId: string, by: string, id: number): Promise<Claim> {
  return postJson<Claim>(`${sheetBase(sheetId)}/claims/${id}/hold?by=${encodeURIComponent(by)}`, {});
}

/** Remind an OPEN or PENDING claim on the sheet's today; refused (rejects) with 409 for any other
 *  status, or when it was already reminded on the sheet's today. */
export function remindClaim(sheetId: string, by: string, id: number): Promise<Claim> {
  return postJson<Claim>(`${sheetBase(sheetId)}/claims/${id}/remind?by=${encodeURIComponent(by)}`, {});
}

/** A steward re-dates an OPEN or PENDING claim to `date`: its due-by and any bound figure stay, and
 *  so do its period (the one its due-by falls in) and its number. Refused (rejects) for anyone else, any other status, a date after its person's
 *  retirement, or a claim due by a day in a closed period. */
export function redateClaim(sheetId: string, by: string, id: number, date: string): Promise<Claim> {
  return postJson<Claim>(`${sheetBase(sheetId)}/claims/${id}/redate?by=${encodeURIComponent(by)}&date=${encodeURIComponent(date)}`, {});
}

/** Split an OPEN claim: it keeps the rest of its amount and loses any figure a statement bound it to;
 *  a new OPEN claim of `amount` (more than 0.00, less than the claim's), opened by the user, is split
 *  from its number. Refused (rejects) for a PENDING, deferred or settled claim, one due by a day in a closed
 *  period, an amount out of range, or a role that may not open a claim. */
export function splitClaim(sheetId: string, by: string, id: number, amount: string): Promise<Claim> {
  return postJson<Claim>(`${sheetBase(sheetId)}/claims/${id}/split?by=${encodeURIComponent(by)}&amount=${encodeURIComponent(amount)}`, {});
}

/** Part-settle an OPEN claim: `amount` is split off as {@link splitClaim} does, and the new claim is settled
 *  at once by the user as the settle does (to PENDING by a clerk above the clerk limit); the four-eyes and
 *  proof rules are judged on the original claim. A refused split or settle refuses it all. Resolves to the new claim. */
export function settlePartClaim(sheetId: string, by: string, id: number, amount: string): Promise<Claim> {
  return postJson<Claim>(`${sheetBase(sheetId)}/claims/${id}/settle-part?by=${encodeURIComponent(by)}&amount=${encodeURIComponent(amount)}`, {});
}

/** The kinds a proof may be (none chosen: a receipt). */
export const PROOF_KINDS = ['receipt', 'photo'] as const;
export type ProofKind = typeof PROOF_KINDS[number];

/** How many of a claim's proofs are of a kind. */
export function proofsOf(c: Pick<Claim, 'proofs' | 'photoProofs'>, kind: string): number {
  const photos = c.photoProofs ?? 0;
  return kind === 'photo' ? photos : kind === 'receipt' ? (c.proofs ?? 0) - photos : 0;
}

/** Raise a claim's proofs count by one, of the kind chosen ('': none, a receipt); refused (rejects) for a SETTLED or VOID claim, or one dated
 *  in a closed period. */
export function addProof(sheetId: string, by: string, id: number, kind: ProofKind | ''): Promise<Claim> {
  const ofKind = kind ? `&kind=${encodeURIComponent(kind)}` : '';
  return postJson<Claim>(`${sheetBase(sheetId)}/claims/${id}/proofs?by=${encodeURIComponent(by)}${ofKind}`, {});
}

/** Comment a claim, by the user on the sheet's today; refused (rejects) for an empty comment. */
export function commentClaim(sheetId: string, by: string, id: number, text: string): Promise<Claim> {
  return postJson<Claim>(`${sheetBase(sheetId)}/claims/${id}/comments?by=${encodeURIComponent(by)}&text=${encodeURIComponent(text)}`, {});
}

/** Add a line to a DRAFT claim (counts as editing it): its amount becomes its lines' quantities times
 *  unit amounts, added up. Refused (rejects) for a claim not a DRAFT, one due by a day in a closed period,
 *  one another user is editing, an empty label, a quantity not a whole number of at least 1, or a unit
 *  amount with more than two decimals. */
export function addClaimLine(sheetId: string, by: string, id: number, line: ClaimLineInput): Promise<Claim> {
  return postJson<Claim>(`${sheetBase(sheetId)}/claims/${id}/lines?by=${encodeURIComponent(by)}&label=${encodeURIComponent(line.label)}`
    + `&qty=${encodeURIComponent(line.qty)}&unit=${encodeURIComponent(line.unit)}`, {});
}

/** A line typed for a claim: its label, quantity and unit amount. */
export type ClaimLineInput = { label: string; qty: string; unit: string };

/** A steward moves a DRAFT, OPEN or PENDING claim to another person (`toPersonId`): it keeps its
 *  number and date, loses any figure a statement bound it to, and is listed as that person's newest
 *  claim. Refused (rejects) for anyone else, for a SETTLED or VOID claim, or one due by a day in a closed period. */
export function reassignClaim(sheetId: string, by: string, id: number, toPersonId: number): Promise<Claim> {
  return postJson<Claim>(`${sheetBase(sheetId)}/claims/${id}/reassign?by=${encodeURIComponent(by)}&to=${toPersonId}`, {});
}

/** Whether a claim counts toward claimed, due-total and every total: only OPEN, PENDING and SETTLED do. */
export const counts = (c: Claim) => isOpen(c) || c.status === 'SETTLED';

/** The claims that count in live totals: all but the deferred claims and the open claims that no statement bound
 *  of a person whose retirement date the sheet's today has reached (`retiredReached`) (SETTLED and bound claims still
 *  count). A hold (the person's or the claim's own) leaves nothing out: it only matters in the pools. */
export function liveClaims(claims: Claim[], people: Array<{ id: number; retiredReached?: boolean }> | undefined): Claim[] {
  const byId = new Map((people ?? []).map((p) => [p.id, p]));
  return claims.filter((c) => !c.deferred && !(isOpen(c) && !c.stated && !!byId.get(c.personId)?.retiredReached));
}

/** The claims that count in claimed and the claimed-total: as `liveClaims`. */
export const claimedClaims = liveClaims;

/** Whether a person is active: one of their claims that count (`live`, see `liveClaims`) is open (OPEN or
 *  PENDING) and dated within the 30 days up to and including the sheet's `today`. */
export function isActive(live: Claim[], today: string): boolean {
  if (today === '') return false;
  const since = new Date(`${today}T00:00:00Z`);
  since.setUTCDate(since.getUTCDate() - 29);
  const from = since.toISOString().slice(0, 10);
  return live.some((c) => isOpen(c) && from <= c.date && c.date <= today);
}

/** A person's patrons' shares (in hundredths, of 100.00), in the order named: 100.00 split equally, each share
 *  rounded half to even to two decimals, the remainder (100.00 less the shares added up) to the first-named. */
export function patronShares(patronIds: number[]): Array<{ id: number; share: number }> {
  const n = patronIds.length;
  if (n === 0) return [];
  const whole = Math.floor(10_000 / n);
  const rest = 10_000 % n;
  const each = 2 * rest > n || (2 * rest === n && whole % 2 === 1) ? whole + 1 : whole;
  return patronIds.map((id, i) => ({ id, share: i === 0 ? 10_000 - each * (n - 1) : each }));
}

/** A person's backing: the sum over their patrons of the patron's figure (the frozen settled figures of their
 *  SETTLED claims dated within the 90 days up to and including the sheet's `today`, added up) times their share
 *  (see `patronShares`) divided by 100, rounded half to even (0.00 with none). */
export function backing(claims: Claim[], patronIds: number[], today: string): string {
  if (today === '') return '0.00';
  const since = new Date(`${today}T00:00:00Z`);
  since.setUTCDate(since.getUTCDate() - 89);
  const from = since.toISOString().slice(0, 10);
  const figures = new Map<number, bigint>(patronIds.map((id) => [id, 0n]));
  for (const c of claims) {
    const f = figures.get(c.personId);
    if (f !== undefined && c.status === 'SETTLED' && c.settledDue !== null && from <= c.date && c.date <= today) {
      figures.set(c.personId, f + BigInt(Math.round(Number(c.settledDue) * 100)));
    }
  }
  // Cents times hundredths of a percent share: 1/10_000 of a cent.
  const product = patronShares(patronIds).reduce((t, p) => t + (figures.get(p.id) ?? 0n) * BigInt(p.share), 0n);
  const cents = halfEven(product < 0n ? -product : product);
  const signed = product < 0n ? -cents : cents;
  return (Number(signed) / 100).toFixed(2);
}

/** A non-negative amount in 1/10_000 of a cent to cents, half to even. */
function halfEven(product: bigint): bigint {
  const whole = product / 10_000n;
  const rest = product % 10_000n;
  return rest > 5_000n || (rest === 5_000n && whole % 2n === 1n) ? whole + 1n : whole;
}

/** Sum the amounts of the claims that count (see `counts`) exactly (in cents), shown with two decimals. */
export function sumAmounts(claims: Claim[]): string {
  const cents = claims.filter(counts).reduce((t, c) => t + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}

/** A claim's due-by (yyyy-MM-dd), fixed when it was added. */
export const dueBy = (c: Claim): string => c.dueBy;

/** Whether a claim is overdue: it is open (OPEN or PENDING), not deferred, and the date its lateness counts
 *  from (`lateFrom`: its due-by plus its grace days; by default its due-by) is before the sheet's `today`. */
export const isOverdue = (c: Claim, today: string, lateFrom: string = dueBy(c)) =>
  isOpen(c) && !c.deferred && today !== '' && lateFrom < today;

/** How many of the selected claims a bulk open opened, and how many it refused (left as they were). */
export type BulkOpenResult = { opened: number; refused: number };

/** Open the selected claims together, numbered in list order (people in their list places, newest claim
 *  first within a person); each that cannot be opened is left as it is and counted as refused. */
export function bulkOpenClaims(sheetId: string, by: string, claimIds: number[]): Promise<BulkOpenResult> {
  return postJson<BulkOpenResult>(`${sheetBase(sheetId)}/bulk-open?by=${encodeURIComponent(by)}`, { claimIds });
}

/** How many of the selected claims a bulk settle settled (one gone to PENDING counts), and how many
 *  it refused (left as they were). */
export type BulkSettleResult = { settled: number; refused: number };

/** Settle the selected claims together in list order (people in their list places, newest claim first
 *  within a person), each by the rules of settling one; each that cannot be settled is left as it is
 *  and counted as refused. */
export function bulkSettleClaims(sheetId: string, by: string, claimIds: number[]): Promise<BulkSettleResult> {
  return postJson<BulkSettleResult>(`${sheetBase(sheetId)}/bulk-settle?by=${encodeURIComponent(by)}`, { claimIds });
}

/** Under the claims' key, so whatever refreshes the claims refreshes it too. */
export const settleBatchKey = (sheetId: string) => [...claimsKey(sheetId), 'settle-batch'] as const;

/** The number of the sheet's last bulk settle batch (null: there has been none). */
export function lastSettleBatch(sheetId: string): Promise<{ last: number | null }> {
  return getJson<{ last: number | null }>(`${sheetBase(sheetId)}/settle-batch`);
}

/** How many claims a paste added. */
export type PasteResult = { added: number };

/** Add a DRAFT claim per line of name,amount,date,note (the note may hold commas, and may end in
 *  ` #<reference>`, the claim's reference, refused when used on the sheet or by another line; a blank date takes
 *  the sheet's entry date when set, else its today; the note may be absent), in order, to the named person; refused (rejects),
 *  adding nothing, when any line would be refused by the claim form or names no one on the sheet. */
export function pasteClaims(sheetId: string, by: string, text: string): Promise<PasteResult> {
  return postJson<PasteResult>(`${sheetBase(sheetId)}/paste-claims?by=${encodeURIComponent(by)}`, { text });
}

/** A reason on the sheet's list of void reasons (trimmed): while the list is not empty, a claim is voided
 *  with one of them. */
export type VoidReason = { sheetId: string; reason: string; by: string };

export const voidReasonsKey = (sheetId: string) => [...sheetKey(sheetId), 'void-reasons'] as const;

/** The sheet's void reasons, in the order they were added. */
export function listVoidReasons(sheetId: string): Promise<VoidReason[]> {
  return getJson<VoidReason[]>(`${sheetBase(sheetId)}/void-reasons`);
}

/** Add a void reason; refused (rejects) for a blank one or one already on the list. */
export function addVoidReason(sheetId: string, by: string, reason: string): Promise<VoidReason> {
  return postJson<VoidReason>(`${sheetBase(sheetId)}/void-reasons?by=${encodeURIComponent(by)}`, { reason });
}

/** How many of the selected claims a bulk reassign moved, and how many it refused (left as they were). */
export type BulkReassignResult = { reassigned: number; refused: number };

/** Move the selected claims to one person (`toPersonId`) together in list order (people in their list places,
 *  newest claim first within a person), each by the rules of reassigning one; each that cannot be moved
 *  is left as it is and counted as refused. */
export function bulkReassignClaims(sheetId: string, by: string, claimIds: number[], toPersonId: number): Promise<BulkReassignResult> {
  return postJson<BulkReassignResult>(`${sheetBase(sheetId)}/bulk-reassign?by=${encodeURIComponent(by)}`, { claimIds, to: toPersonId });
}

/** How many of the selected claims a bulk re-date re-dated, and how many it refused (left as they were). */
export type BulkRedateResult = { redated: number; refused: number };

/** Re-date the selected claims to one `date` together in list order (people in their list places, newest
 *  claim first within a person), each by the rules of re-dating one; each that cannot be re-dated is left
 *  as it is and counted as refused. */
export function bulkRedateClaims(sheetId: string, by: string, claimIds: number[], date: string): Promise<BulkRedateResult> {
  return postJson<BulkRedateResult>(`${sheetBase(sheetId)}/bulk-redate?by=${encodeURIComponent(by)}`, { claimIds, date });
}

export type BulkVoidResult = { voided: number; refused: number };

/** Void the selected claims together in list order (people in their list places, newest claim first
 *  within a person), each by the rules of voiding one with `reason` (needed while the sheet has void
 *  reasons); each that cannot be voided is left as it is and counted as refused. */
export function bulkVoidClaims(sheetId: string, by: string, claimIds: number[], reason: string): Promise<BulkVoidResult> {
  return postJson<BulkVoidResult>(`${sheetBase(sheetId)}/bulk-void?by=${encodeURIComponent(by)}`, { claimIds, reason });
}
