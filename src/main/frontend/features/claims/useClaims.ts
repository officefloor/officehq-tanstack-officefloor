import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A claim's status: DRAFT -> OPEN -> SETTLED; a DRAFT or OPEN claim can become VOID. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';

/** A claim: its amount with two decimals, its date (yyyy-mm-dd), its note and its status. */
export type Claim = { id: number; personId: number; amount: string; date: string; note: string; status: ClaimStatus };

/** Whether a claim counts toward claimed and every total: only OPEN and SETTLED claims do. */
export function counts(c: Claim): boolean {
  return c.status === 'OPEN' || c.status === 'SETTLED';
}

export function claimsKey(sheetId: string) {
  return ['sheet', sheetId, 'claims'];
}

/** Every claim on the sheet, newest first. */
export function useClaims(sheetId: string) {
  return useQuery({
    queryKey: claimsKey(sheetId),
    queryFn: async () =>
      (await getJson<{ claims: Claim[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/claims`)).claims,
  });
}

/** The sum of the amounts of those of these claims that count, with two decimals (added up in
 *  cents, so no rounding drift). */
export function claimed(claims: Claim[]): string {
  const cents = claims.filter(counts).reduce((sum, c) => sum + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
