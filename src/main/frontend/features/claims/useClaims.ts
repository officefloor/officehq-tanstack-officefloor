import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asNumber, useSearchParam } from '../../url/useSearchParam';

export type Claim = { id: number; personId: number; amount: string; date: string; note: string };

/** The key every feature reads the sheet's claims under. */
export const claimsKey = (sheetId: string) => ['sheet', sheetId, 'claims'] as const;

/** Every claim on the sheet, newest first. */
export function useClaims(sheetId: string) {
  return useQuery({
    queryKey: claimsKey(sheetId),
    queryFn: () => getJson<Claim[]>(`/api/sheets/${encodeURIComponent(sheetId)}/claims`),
  });
}

/** Which claim's editor is open (search param `editClaim`); none means each claim form adds. */
export function useEditingClaim() {
  return useSearchParam('editClaim', asNumber);
}

/** Amounts added up exactly, in cents, shown with two decimals. */
export function sumAmounts(claims: Claim[]): string {
  const cents = claims.reduce((n, c) => n + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
