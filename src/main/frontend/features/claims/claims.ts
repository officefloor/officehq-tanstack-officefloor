import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';

/** A person's claim: the amount with two decimals, the date (yyyy-mm-dd) and a note (may be empty). */
export type Claim = { id: number; personId: number; amount: string; date: string; note: string };

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

/** Amounts added up exactly (in cents) and shown with two decimals. */
export function sumAmounts(claims: Claim[]): string {
  const cents = claims.reduce((total, c) => total + Math.round(Number(c.amount) * 100), 0);
  return `${Math.floor(cents / 100)}.${String(cents % 100).padStart(2, '0')}`;
}
