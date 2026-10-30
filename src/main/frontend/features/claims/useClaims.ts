import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';

/** A claim; `amount` is shown with two decimals ("10.25"); `number` is the one given when it was
 *  opened, "" for a draft or a voided claim; `due` is the due fixed when it was settled, "" while
 *  its due is live (worked from the current rate). */
export type Claim = {
  id: string;
  personId: string;
  amount: string;
  date: string;
  note: string;
  status: ClaimStatus;
  number: string;
  due: string;
};

/** Only OPEN and SETTLED claims count toward claimed, due-total and every total. */
export const counts = (c: Claim) => c.status === 'OPEN' || c.status === 'SETTLED';

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

/** The amounts of the claims that count added up, shown with two decimals. Summed in whole cents
 *  so no float drift shows. */
export function sumAmounts(claims: Claim[]): string {
  const cents = claims.filter(counts).reduce((sum, c) => sum + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
