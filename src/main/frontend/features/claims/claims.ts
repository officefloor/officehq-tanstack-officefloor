import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheet';

/** `amount` has two decimals; `date` is yyyy-MM-dd. */
export type Claim = { id: number; personId: number; amount: string; date: string; note: string };

/** Under the sheet's key, so loading the sheet again re-reads its claims. */
export const claimsKey = (sheet: string) => [...sheetKey(sheet), 'claims'] as const;

/** Every claim on the sheet, newest first. */
export function useClaims(sheet: string) {
  return useQuery({
    queryKey: claimsKey(sheet),
    queryFn: () => getJson<Claim[]>(`/api/sheets/${encodeURIComponent(sheet)}/claims`),
    enabled: sheet !== '',
  });
}

/** Amounts are added in whole cents, so the sum is exact. */
const cents = (amount: string) => Math.round(Number(amount) * 100);

/** The sum of these claims' amounts, with two decimals. */
export function claimed(claims: Claim[]): string {
  return (claims.reduce((sum, c) => sum + cents(c.amount), 0) / 100).toFixed(2);
}
