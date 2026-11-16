import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A claim of a person. `amount` is text with two decimals (never a float); `date` is yyyy-MM-dd. */
export type Claim = { id: number; sheetId: string; personId: number; amount: string; date: string; note: string };

export const claimsKey = (sheetId: string) => ['sheet', sheetId, 'claims'];

/** Every claim on the sheet, newest first. */
export function useClaims(sheetId: string) {
  return useQuery({
    queryKey: claimsKey(sheetId),
    queryFn: () => getJson<Claim[]>(`/api/sheets/${encodeURIComponent(sheetId)}/claims`),
  });
}

/** An amount ("12.34") in whole cents, so sums are exact. */
const cents = (amount: string) => Math.round(Number(amount) * 100);

/** The amounts added up, shown with two decimals. */
export function total(claims: Claim[]): string {
  return (claims.reduce((sum, c) => sum + cents(c.amount), 0) / 100).toFixed(2);
}
