import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';

/** Where a claim is in its life: DRAFT, then OPEN, then SETTLED; a DRAFT or OPEN claim may be VOID. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';

/** A person's claim: the amount with two decimals, the date (yyyy-mm-dd), a note (may be empty), its status
 *  and its number on the sheet (null while a draft, or once voided). */
export type Claim = { id: number; personId: number; amount: string; date: string; note: string; status: ClaimStatus;
  number: number | null };

/** Whether a claim counts toward claimed, due-total and every total: only OPEN and SETTLED ones do. */
export function counts(c: Claim): boolean {
  return c.status === 'OPEN' || c.status === 'SETTLED';
}

export const claimsKey = (sheetId: string) => ['sheet', sheetId, 'claims'] as const;

/** Every claim on the sheet, newest first. */
export function useClaims(sheetId: string) {
  return useQuery({
    queryKey: claimsKey(sheetId),
    queryFn: () => getJson<Claim[]>(`/api/sheets/${encodeURIComponent(sheetId)}/claims`),
  });
}

/** The id of the claim whose editor is open ('' when each person's claim form adds a new claim). */
export function useEditingClaim() {
  return useSearchParam('editClaim', asString);
}

/** The amounts of the claims that count (OPEN and SETTLED) added up exactly (in cents) and shown
 *  with two decimals. */
export function sumAmounts(claims: Claim[]): string {
  const cents = claims.filter(counts).reduce((total, c) => total + Math.round(Number(c.amount) * 100), 0);
  return `${Math.floor(cents / 100)}.${String(cents % 100).padStart(2, '0')}`;
}
