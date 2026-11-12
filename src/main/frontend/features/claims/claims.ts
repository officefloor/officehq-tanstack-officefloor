import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheet';

export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';

/** `amount` has two decimals; `date` is yyyy-MM-dd; `number` is given when the claim is opened
 *  (null for a draft or a voided claim). */
export type Claim = {
  id: number;
  personId: number;
  amount: string;
  date: string;
  note: string;
  status: ClaimStatus;
  number: number | null;
  /** The due fixed when the claim was settled; null when it has none, so its due follows the rate. */
  due: string | null;
  /** The user who opened the claim; null until it is opened. */
  openedBy: string | null;
  /** Whether the person's last statement bound the claim to its figure then. */
  stated: boolean;
  /** The figure the claim was bound to; null when not bound, or bound while it had none. */
  statedDue: string | null;
};

/** Only OPEN and SETTLED claims count toward claimed and the totals. */
export const counts = (c: Claim) => c.status === 'OPEN' || c.status === 'SETTLED';

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

/** The sum of the amounts of those of these claims that count, with two decimals. */
export function claimed(claims: Claim[]): string {
  return (claims.filter(counts).reduce((sum, c) => sum + cents(c.amount), 0) / 100).toFixed(2);
}
