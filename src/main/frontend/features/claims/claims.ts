import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A claim's status: DRAFT when added, then OPEN and SETTLED; a DRAFT or OPEN claim may be VOID. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';

/** A claim of a person. `amount` is text with two decimals (never a float); `date` is yyyy-MM-dd; `number` is given
 *  when the claim is opened (null for a draft or a voided claim); `due` is the due fixed when it was settled (null
 *  while its due is live, worked out from its grade's current rate). */
export type Claim = {
  id: number;
  sheetId: string;
  personId: number;
  amount: string;
  date: string;
  note: string;
  status: ClaimStatus;
  number: number | null;
  due: string | null;
};

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

/** Whether a claim counts toward claimed and the totals: only OPEN and SETTLED claims do. */
export const counts = (c: Claim) => c.status === 'OPEN' || c.status === 'SETTLED';

/** The amounts of the claims that count added up, shown with two decimals. */
export function total(claims: Claim[]): string {
  return (claims.filter(counts).reduce((sum, c) => sum + cents(c.amount), 0) / 100).toFixed(2);
}
