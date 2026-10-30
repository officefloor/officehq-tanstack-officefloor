import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A claim; `amount` is shown with two decimals ("10.25"). */
export type Claim = { id: string; personId: string; amount: string; date: string; note: string };

export const claimsKey = (sheet: string) => ['sheet', sheet, 'claims'] as const;

/** The sheet's claims, newest first. */
export function useClaims(sheet: string): Claim[] {
  const { data } = useQuery({
    queryKey: claimsKey(sheet),
    queryFn: () => getJson<Claim[]>(`/api/sheets/${encodeURIComponent(sheet)}/claims`),
    enabled: sheet !== '',
  });
  return data ?? [];
}

/** The amounts added up, shown with two decimals. Summed in whole cents so no float drift shows. */
export function sumAmounts(claims: Claim[]): string {
  const cents = claims.reduce((sum, c) => sum + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
