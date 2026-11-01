import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** Where a claim is in its life: DRAFT -> OPEN -> SETTLED, or DRAFT/OPEN -> VOID. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';

/** A status change a claim's row offers; the server refuses one its status does not allow. */
export type ClaimStatusChange = 'open' | 'settle' | 'void';

/** A claim: its amount with two decimals, its date (yyyy-MM-dd), its note, its status and its number on
 * the sheet (given when it is opened; null while a draft and once voided). */
export type Claim = {
  id: number;
  person: number;
  amount: string;
  date: string;
  note: string;
  status: ClaimStatus;
  number: number | null;
};

export const claimsKey = (sheet: string) => ['claims', sheet] as const;

export const claimsPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/claims`;

/** Every claim on the sheet, newest first. */
export function useClaims(sheet: string) {
  return useQuery({
    queryKey: claimsKey(sheet),
    queryFn: () => getJson<Claim[]>(claimsPath(sheet)),
  });
}

/** The claims that count toward claimed, due-total and every total: the OPEN and SETTLED ones. */
export function counted(claims: Claim[]): Claim[] {
  return claims.filter((c) => c.status === 'OPEN' || c.status === 'SETTLED');
}

/** The sum of the claims' amounts with two decimals, added in whole cents so no float error shows. */
export function claimed(claims: Claim[]): string {
  const cents = claims.reduce((sum, c) => {
    const [whole, frac = ''] = c.amount.split('.');
    return sum + Number(whole) * 100 + Number(frac.padEnd(2, '0'));
  }, 0);
  return `${Math.floor(cents / 100)}.${String(cents % 100).padStart(2, '0')}`;
}
