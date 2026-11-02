import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A claim: its amount with two decimals, its date (yyyy-mm-dd) and its note. */
export type Claim = { id: number; personId: number; amount: string; date: string; note: string };

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

/** The sum of these claims' amounts, with two decimals (added up in cents, so no rounding drift). */
export function claimed(claims: Claim[]): string {
  const cents = claims.reduce((sum, c) => sum + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
