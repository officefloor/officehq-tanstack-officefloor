import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A claim: its amount with two decimals, its date (yyyy-MM-dd) and its note. */
export type Claim = { id: number; person: number; amount: string; date: string; note: string };

export const claimsKey = (sheet: string) => ['claims', sheet] as const;

export const claimsPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/claims`;

/** Every claim on the sheet, newest first. */
export function useClaims(sheet: string) {
  return useQuery({
    queryKey: claimsKey(sheet),
    queryFn: () => getJson<Claim[]>(claimsPath(sheet)),
  });
}

/** The sum of the claims' amounts with two decimals, added in whole cents so no float error shows. */
export function claimed(claims: Claim[]): string {
  const cents = claims.reduce((sum, c) => {
    const [whole, frac = ''] = c.amount.split('.');
    return sum + Number(whole) * 100 + Number(frac.padEnd(2, '0'));
  }, 0);
  return `${Math.floor(cents / 100)}.${String(cents % 100).padStart(2, '0')}`;
}
