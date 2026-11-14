import { getJson, postJson, putJson } from '../../api/http';
import { sheetKey } from '../sheet/api';

/** A claim's status: DRAFT, opened to OPEN, settled to SETTLED; a DRAFT or OPEN claim can be voided. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';
/** A change of a claim's status. */
export type ClaimAction = 'open' | 'settle' | 'void';

/** A claim: its amount as two-decimal text, its date (yyyy-MM-dd), its note, its status and its
 *  number on the sheet (given when opened; null while a draft or once voided), and its settled due
 *  (fixed when settled; null otherwise, or when there was no rate then — its due stays live), and who
 *  opened it (null while a draft; that user cannot settle it), and whether its person's last statement
 *  bound it, with the figure it bound (null when it had none): a bound claim shows and totals that.
 *  A correction (a negative amount) has the number of the SETTLED claim it corrects (null otherwise).
 *  `roundDate` is the date its live figures round by: its own, or a correction's claim's it corrects. */
export type Claim = {
  id: number; sheetId: string; personId: number; amount: string; date: string; note: string; status: ClaimStatus;
  number: number | null; settledDue: string | null; openedBy: string | null;
  stated: boolean; statedDue: string | null; corrects: number | null; roundDate: string;
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

/** Change a claim's status; refused (rejects) when its status does not allow the change, or when the
 *  user settling it is the one who opened it. */
export function moveClaim(sheetId: string, by: string, id: number, action: ClaimAction): Promise<Claim> {
  return postJson<Claim>(`${sheetBase(sheetId)}/claims/${id}/${action}?by=${encodeURIComponent(by)}`, {});
}

/** Whether a claim counts toward claimed, due-total and every total: only OPEN and SETTLED do. */
export const counts = (c: Claim) => c.status === 'OPEN' || c.status === 'SETTLED';

/** The claims that count in live totals: all but the OPEN claims of people on hold (their SETTLED
 *  claims still count). */
export function liveClaims(claims: Claim[], people: Array<{ id: number; held: boolean }> | undefined): Claim[] {
  const held = new Set((people ?? []).filter((p) => p.held).map((p) => p.id));
  return claims.filter((c) => !(c.status === 'OPEN' && held.has(c.personId)));
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

/** Whether a claim is overdue: it is OPEN and its due-by is before the sheet's `today`. */
export const isOverdue = (c: Claim, terms: number, today: string) =>
  c.status === 'OPEN' && today !== '' && dueBy(c, terms) < today;
