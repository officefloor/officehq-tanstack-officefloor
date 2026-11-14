import { getJson, postJson, putJson } from '../../api/http';
import { sheetKey } from '../sheet/api';

/** A claim: its amount as two-decimal text, its date (yyyy-MM-dd) and its note. */
export type Claim = { id: number; sheetId: string; personId: number; amount: string; date: string; note: string };
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

/** Sum two-decimal amounts exactly (in cents), shown with two decimals. */
export function sumAmounts(claims: Claim[]): string {
  const cents = claims.reduce((t, c) => t + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
