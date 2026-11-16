import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import type { Person } from '../people/people';

/** A claim's status: DRAFT when added, then OPEN and SETTLED; a DRAFT or OPEN claim may be VOID. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';

/** A claim of a person. `amount` is text with two decimals (never a float); `date` is yyyy-MM-dd; `number` is given
 *  when the claim is opened (null for a draft or a voided claim); `due` is the due fixed when it was settled (null
 *  while its due is live, worked out from its grade's current rate); `openedBy` is who opened it (null when never
 *  opened); `stated` is whether its person's last statement bound it to the figure `statedDue` (null when it had
 *  none), which it shows while its due is not fixed; `corrects` is, for a correction (a negative amount),
 *  the number of the SETTLED claim of the same person it corrects (null for any other claim). */
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
  openedBy: string | null;
  stated: boolean;
  statedDue: string | null;
  corrects: number | null;
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

/** The claims that count in the live totals (claimed, due-totals, a period's due-total): a held person's OPEN claims
 *  do not; their SETTLED ones still do. */
export function live(claims: Claim[], people: Person[] | undefined): Claim[] {
  const held = new Set((people ?? []).filter((p) => p.held).map((p) => p.id));
  return claims.filter((c) => counts(c) && !(c.status === 'OPEN' && held.has(c.personId)));
}

/** The amounts of the claims that count added up, shown with two decimals. */
export function total(claims: Claim[]): string {
  return (claims.filter(counts).reduce((sum, c) => sum + cents(c.amount), 0) / 100).toFixed(2);
}
