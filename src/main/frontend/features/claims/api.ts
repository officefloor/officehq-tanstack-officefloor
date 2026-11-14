import { getJson, postJson, putJson } from '../../api/http';
import { sheetKey } from '../sheet/api';

/** A claim's status: DRAFT, opened to OPEN, settled to SETTLED (or, by a clerk above the clerk limit,
 *  to PENDING, which a steward approves to SETTLED or rejects back to OPEN); a DRAFT, OPEN or PENDING
 *  claim can be voided. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID';
/** A change of a claim's status. */
export type ClaimAction = 'open' | 'settle' | 'void' | 'approve' | 'reject';

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
 *  `roundDate` is the date its live figures round by: its own, or a correction's claim's it corrects. */
export type Claim = {
  id: number; sheetId: string; personId: number; amount: string; date: string; note: string; status: ClaimStatus;
  number: string | null; settledDue: string | null; openedBy: string | null;
  stated: boolean; statedDue: string | null; corrects: string | null; roundDate: string;
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
 *  SETTLED claim of the person. A blank date takes the sheet's today. */
export function addClaim(sheetId: string, by: string, personId: number, input: ClaimInput): Promise<Claim> {
  return postJson<Claim>(`${sheetBase(sheetId)}/people/${personId}/claims?by=${encodeURIComponent(by)}`, input);
}

/** Edit a claim; refused (rejects) like an add. */
export function updateClaim(sheetId: string, by: string, id: number, input: ClaimInput): Promise<Claim> {
  return putJson<Claim>(`${sheetBase(sheetId)}/claims/${id}?by=${encodeURIComponent(by)}`, input);
}

/** Change a claim's status; refused (rejects) when its status does not allow the change, when the
 *  user settling or approving it is the one who opened it, when a non-steward approves or rejects it,
 *  or when a rejection has no `reason`. */
export function moveClaim(sheetId: string, by: string, id: number, action: ClaimAction, reason = ''): Promise<Claim> {
  const extra = action === 'reject' ? `&reason=${encodeURIComponent(reason)}` : '';
  return postJson<Claim>(`${sheetBase(sheetId)}/claims/${id}/${action}?by=${encodeURIComponent(by)}${extra}`, {});
}

/** A steward moves a DRAFT, OPEN or PENDING claim to another person (`toPersonId`): it keeps its
 *  number and date, loses any figure a statement bound it to, and is listed as that person's newest
 *  claim. Refused (rejects) for anyone else, for a SETTLED or VOID claim, or one dated in a closed period. */
export function reassignClaim(sheetId: string, by: string, id: number, toPersonId: number): Promise<Claim> {
  return postJson<Claim>(`${sheetBase(sheetId)}/claims/${id}/reassign?by=${encodeURIComponent(by)}&to=${toPersonId}`, {});
}

/** Whether a claim counts toward claimed, due-total and every total: only OPEN, PENDING and SETTLED do. */
export const counts = (c: Claim) => isOpen(c) || c.status === 'SETTLED';

/** The claims that count in live totals: all but the open claims of people on hold (their SETTLED
 *  claims still count). */
export function liveClaims(claims: Claim[], people: Array<{ id: number; held: boolean }> | undefined): Claim[] {
  const held = new Set((people ?? []).filter((p) => p.held).map((p) => p.id));
  return claims.filter((c) => !(isOpen(c) && held.has(c.personId)));
}

/** Sum the amounts of the claims that count (see `counts`) exactly (in cents), shown with two decimals. */
export function sumAmounts(claims: Claim[]): string {
  const cents = claims.filter(counts).reduce((t, c) => t + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}

/** A claim's due-by (yyyy-MM-dd): its date plus the sheet's terms in days. */
export function dueBy(c: Claim, terms: number): string {
  const d = new Date(`${c.date}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + terms);
  return d.toISOString().slice(0, 10);
}

/** Whether a claim is overdue: it is open (OPEN or PENDING) and its due-by is before the sheet's `today`. */
export const isOverdue = (c: Claim, terms: number, today: string) =>
  isOpen(c) && today !== '' && dueBy(c, terms) < today;

/** How many of the selected claims a bulk open opened, and how many it refused (left as they were). */
export type BulkOpenResult = { opened: number; refused: number };

/** Open the selected claims together, numbered in list order (people in added order, newest claim
 *  first within a person); each that cannot be opened is left as it is and counted as refused. */
export function bulkOpenClaims(sheetId: string, by: string, claimIds: number[]): Promise<BulkOpenResult> {
  return postJson<BulkOpenResult>(`${sheetBase(sheetId)}/bulk-open?by=${encodeURIComponent(by)}`, { claimIds });
}
