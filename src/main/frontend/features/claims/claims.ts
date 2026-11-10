import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A claim: its amount with two decimals, its date (yyyy-MM-dd) and its note. */
export type Claim = { id: string; personId: string; amount: string; date: string; note: string };

export const claimsKey = (sheetId: string) => ['sheet', sheetId, 'claims'] as const;

/** The sheet's claims, newest first. */
export function useClaims(sheetId: string) {
  return useQuery({
    queryKey: claimsKey(sheetId),
    queryFn: async () =>
      (await getJson<{ claims: Claim[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/claims`)).claims,
  });
}

/** An amount as a claim takes it: 0.00 or more, with up to two decimals. */
export const AMOUNT = /^\d+(\.\d{1,2})?$/;

/** The amounts added up, in whole cents so no rounding creeps in, shown with two decimals. */
export function sumAmounts(claims: Claim[]): string {
  const cents = claims.reduce((n, c) => n + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
