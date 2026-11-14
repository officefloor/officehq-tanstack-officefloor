import { getJson, postJson, putJson } from '../../api/http';
import { sheetKey } from '../sheet/api';

/** A claim's status: DRAFT, opened to OPEN, settled to SETTLED; a DRAFT or OPEN claim can be voided. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';
/** A change of a claim's status. */
export type ClaimAction = 'open' | 'settle' | 'void';

/** A claim: its amount as two-decimal text, its date (yyyy-MM-dd), its note, its status and its
 *  number on the sheet (given when opened; null while a draft or once voided), and its settled due
 *  (fixed when settled; null otherwise, or when there was no rate then — its due stays live), and who
 *  opened it (null while a draft; that user cannot settle it). */
export type Claim = {
  id: number; sheetId: string; personId: number; amount: string; date: string; note: string; status: ClaimStatus;
  number: number | null; settledDue: string | null; openedBy: string | null;
};
export type ClaimInput = { amount: string; date: string; note: string };

const sheetBase = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}`;

export const claimsKey = (sheetId: string) => [...sheetKey(sheetId), 'claims'] as const;

/** Every claim on the sheet, newest first. */
export function listClaims(sheetId: string): Promise<Claim[]> {
  return getJson<Claim[]>(`${sheetBase(sheetId)}/claims`);
}

/** Add a claim to a person; refused (rejects) when the amount is missing, negative or has more
 *  than two decimals, or the date is not a date. A blank date takes the sheet's today. */
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

/** Sum the amounts of the claims that count (see `counts`) exactly (in cents), shown with two decimals. */
export function sumAmounts(claims: Claim[]): string {
  const cents = claims.filter(counts).reduce((t, c) => t + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
