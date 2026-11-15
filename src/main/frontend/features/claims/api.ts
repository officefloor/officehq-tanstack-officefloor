import { getJson, postJson, putJson } from '../../api/http';
import { sheetKey } from '../sheet/api';

/** A claim's status: DRAFT, opened to OPEN, settled to SETTLED (or, by a clerk above the clerk limit,
 *  to PENDING, which a steward approves to SETTLED or rejects back to OPEN); a DRAFT, OPEN or PENDING
 *  claim can be voided, and a SETTLED one by a steward within 7 days of its settlement. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID';
/** A change of a claim's status. */
export type ClaimAction = 'open' | 'settle' | 'void' | 'approve' | 'reject' | 'resettle';

/** Whether a claim is open: OPEN, or PENDING a steward's approval. */
export const isOpen = (c: Pick<Claim, 'status'>) => c.status === 'OPEN' || c.status === 'PENDING';

/** Whether a statement's figure binds a claim now: it was bound to one and is not PENDING (a PENDING
 *  claim shows and totals its live figure until it is rejected). */
export const isBound = (c: Claim) => c.stated && c.statedDue !== null && c.status !== 'PENDING';

/** A claim: its amount as two-decimal text, its date (yyyy-MM-dd), its note, its status and its
 *  number on the sheet (given when opened: the sheet-wide 1, 2, ... or, under period numbering,
 *  <period>-<n> or X-<n>; null while a draft), and its settled due
 *  (fixed when settled; null otherwise, or when there was no rate then — its due stays live), and who
 *  opened it (null while a draft; that user cannot settle it), and whether its person's last statement
 *  bound it, with the figure it bound (null when it had none): a bound claim shows and totals that.
 *  A correction (a negative amount) has the number of the SETTLED claim it corrects (null otherwise).
 *  `roundDate` is the date its live figures round by: its own, or a correction's claim's it corrects.
 *  `dueBy` (yyyy-MM-dd) is fixed when it was added: its date then plus the terms of its person's
 *  grade in force on that date (the grade's own, else the sheet's). `log` is its status log, oldest
 *  first: `<STATUS> by <user> on <date> due=<due>`, the sheet's today and its due at each change.
 *  `deferredUntil` is the date it was last deferred until (null: never), and `deferred` whether it is
 *  deferred on the sheet's today (OPEN and deferred until a later date): a deferred claim counts in no
 *  total, is not overdue, cannot be settled and is left unbound by a statement. `proofs` is how many
 *  proofs it has: one whose due is above the proof threshold in force on its date cannot be settled or
 *  approved with none. */
export type Claim = {
  id: number; sheetId: string; personId: number; amount: string; date: string; note: string; status: ClaimStatus;
  number: string | null; settledDue: string | null; openedBy: string | null;
  stated: boolean; statedDue: string | null; corrects: string | null; roundDate: string; dueBy: string;
  log: string[]; deferredUntil: string | null; deferred: boolean; proofs: number;
  /** The number of the claim it was split from (null: not split off a claim). */
  splitFrom: string | null;
  /** The user whose open editor locks it for everyone else (null: unlocked). */
  lockedBy: string | null;
  /** Whether it was added while the period its due-by falls in was soft-closed. */
  lateEntry: boolean;
  /** The last sheet's today it was reminded on (null: never reminded). */
  reminded: string | null;
};
/** `corrects`: the number of the SETTLED claim of the same person a correction (a negative amount) corrects. */
export type ClaimInput = { amount: string; date: string; note: string; corrects: string };

/** Whether a claim is a correction: its amount is negative. */
export const isCorrection = (c: Pick<Claim, 'amount'>) => c.amount.trim().startsWith('-') && Number(c.amount) !== 0;

const sheetBase = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}`;

export const claimsKey = (sheetId: string) => [...sheetKey(sheetId), 'claims'] as const;

/** Every claim on the sheet, newest first. */
export function listClaims(sheetId: string): Promise<Claim[]> {
  return getJson<Claim[]>(`${sheetBase(sheetId)}/claims`);
}

/** Add a claim to a person; refused (rejects) when the amount is missing or has more than two
 *  decimals, the date is not a date, or it is a correction (negative) that does not name by number a
 *  SETTLED claim of the person. A blank date takes the sheet's entry date when set, else its today. */
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
 *  or when a rejection has no `reason`. A steward's `resettle` freezes a SETTLED claim again at the
 *  figure it would have today as an open claim bound by no statement. */
export function moveClaim(sheetId: string, by: string, id: number, action: ClaimAction, reason = ''): Promise<Claim> {
  const extra = action === 'reject' ? `&reason=${encodeURIComponent(reason)}` : '';
  return postJson<Claim>(`${sheetBase(sheetId)}/claims/${id}/${action}?by=${encodeURIComponent(by)}${extra}`, {});
}

/** Defer an OPEN claim until a date after the sheet's today; refused (rejects) when it is not OPEN,
 *  the date is not after today, or it is due by a day in a closed period. */
export function deferClaim(sheetId: string, by: string, id: number, until: string): Promise<Claim> {
  return postJson<Claim>(`${sheetBase(sheetId)}/claims/${id}/defer?by=${encodeURIComponent(by)}&until=${encodeURIComponent(until)}`, {});
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

/** Raise a claim's proofs count by one; refused (rejects) for a SETTLED or VOID claim, or one dated
 *  in a closed period. */
export function addProof(sheetId: string, by: string, id: number): Promise<Claim> {
  return postJson<Claim>(`${sheetBase(sheetId)}/claims/${id}/proofs?by=${encodeURIComponent(by)}`, {});
}

/** A steward moves a DRAFT, OPEN or PENDING claim to another person (`toPersonId`): it keeps its
 *  number and date, loses any figure a statement bound it to, and is listed as that person's newest
 *  claim. Refused (rejects) for anyone else, for a SETTLED or VOID claim, or one due by a day in a closed period. */
export function reassignClaim(sheetId: string, by: string, id: number, toPersonId: number): Promise<Claim> {
  return postJson<Claim>(`${sheetBase(sheetId)}/claims/${id}/reassign?by=${encodeURIComponent(by)}&to=${toPersonId}`, {});
}

/** Whether a claim counts toward claimed, due-total and every total: only OPEN, PENDING and SETTLED do. */
export const counts = (c: Claim) => isOpen(c) || c.status === 'SETTLED';

/** The claims that count in live totals: all but the deferred claims and the open claims that no statement bound,
 *  dated within one of their person's holds (`to` null: open-ended) or of a person whose retirement date the sheet's
 *  today has reached (`retiredReached`) (SETTLED and bound claims still count). */
export function liveClaims(claims: Claim[],
  people: Array<{ id: number; holds: Array<{ from: string; to: string | null }>; retiredReached?: boolean }> | undefined): Claim[] {
  const byId = new Map((people ?? []).map((p) => [p.id, p]));
  const held = (c: Claim) => {
    const p = byId.get(c.personId);
    return !!p && (!!p.retiredReached || p.holds.some((h) => h.from <= c.date && (h.to === null || c.date <= h.to)));
  };
  return claims.filter((c) => !c.deferred && !(isOpen(c) && !c.stated && held(c)));
}

/** Whether a person is active: one of their claims that count (`live`, see `liveClaims`) is open (OPEN or
 *  PENDING) and dated within the 30 days up to and including the sheet's `today`. */
export function isActive(live: Claim[], today: string): boolean {
  if (today === '') return false;
  const since = new Date(`${today}T00:00:00Z`);
  since.setUTCDate(since.getUTCDate() - 29);
  const from = since.toISOString().slice(0, 10);
  return live.some((c) => isOpen(c) && from <= c.date && c.date <= today);
}

/** A person's backing: the frozen settled figures of their patrons' SETTLED claims dated within the 90 days up to
 *  and including the sheet's `today`, added up (0.00 with none). */
export function backing(claims: Claim[], patronIds: number[], today: string): string {
  if (today === '') return '0.00';
  const since = new Date(`${today}T00:00:00Z`);
  since.setUTCDate(since.getUTCDate() - 89);
  const from = since.toISOString().slice(0, 10);
  const patrons = new Set(patronIds);
  const cents = claims
    .filter((c) => patrons.has(c.personId) && c.status === 'SETTLED' && c.settledDue !== null && from <= c.date && c.date <= today)
    .reduce((t, c) => t + Math.round(Number(c.settledDue) * 100), 0);
  return (cents / 100).toFixed(2);
}

/** Sum the amounts of the claims that count (see `counts`) exactly (in cents), shown with two decimals. */
export function sumAmounts(claims: Claim[]): string {
  const cents = claims.filter(counts).reduce((t, c) => t + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}

/** A claim's due-by (yyyy-MM-dd), fixed when it was added. */
export const dueBy = (c: Claim): string => c.dueBy;

/** Whether a claim is overdue: it is open (OPEN or PENDING), not deferred, and its due-by is before the sheet's `today`. */
export const isOverdue = (c: Claim, today: string) =>
  isOpen(c) && !c.deferred && today !== '' && dueBy(c) < today;

/** How many of the selected claims a bulk open opened, and how many it refused (left as they were). */
export type BulkOpenResult = { opened: number; refused: number };

/** Open the selected claims together, numbered in list order (people in added order, newest claim
 *  first within a person); each that cannot be opened is left as it is and counted as refused. */
export function bulkOpenClaims(sheetId: string, by: string, claimIds: number[]): Promise<BulkOpenResult> {
  return postJson<BulkOpenResult>(`${sheetBase(sheetId)}/bulk-open?by=${encodeURIComponent(by)}`, { claimIds });
}

/** How many of the selected claims a bulk settle settled (one gone to PENDING counts), and how many
 *  it refused (left as they were). */
export type BulkSettleResult = { settled: number; refused: number };

/** Settle the selected claims together in list order (people in added order, newest claim first
 *  within a person), each by the rules of settling one; each that cannot be settled is left as it is
 *  and counted as refused. */
export function bulkSettleClaims(sheetId: string, by: string, claimIds: number[]): Promise<BulkSettleResult> {
  return postJson<BulkSettleResult>(`${sheetBase(sheetId)}/bulk-settle?by=${encodeURIComponent(by)}`, { claimIds });
}

/** How many claims a paste added. */
export type PasteResult = { added: number };

/** Add a DRAFT claim per line of name,amount,date,note (the note may hold commas; a blank date takes
 *  the sheet's entry date when set, else its today; the note may be absent), in order, to the named person; refused (rejects),
 *  adding nothing, when any line would be refused by the claim form or names no one on the sheet. */
export function pasteClaims(sheetId: string, by: string, text: string): Promise<PasteResult> {
  return postJson<PasteResult>(`${sheetBase(sheetId)}/paste-claims?by=${encodeURIComponent(by)}`, { text });
}
