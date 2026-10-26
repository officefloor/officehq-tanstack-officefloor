import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheetQuery';

/** `amount` has two decimals ("12.50"); `date` is yyyy-mm-dd. */
export type Claim = { id: number; personId: number; amount: string; date: string; note: string };

/** Under the sheet's key, so re-loading the sheet re-reads its claims. */
export const claimsKey = (sheetId: string) => [...sheetKey(sheetId), 'claims'] as const;

/** Every claim on the sheet, newest first. */
export const claimsQuery = (sheetId: string) => ({
  queryKey: claimsKey(sheetId),
  queryFn: () => getJson<Claim[]>(`/api/sheets/${encodeURIComponent(sheetId)}/claims`),
});

/** Amounts are added up in whole cents, so the sum shows exactly with two decimals. */
export function sumAmounts(claims: Claim[]): string {
  const cents = claims.reduce((total, c) => total + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
