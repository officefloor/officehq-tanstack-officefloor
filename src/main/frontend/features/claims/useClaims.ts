import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { dueCents, hundredths } from '../rates/useRates';

/** Where a claim is in its life: DRAFT → OPEN → SETTLED, and DRAFT or OPEN → VOID. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';

/** A claim as the server sends it: `amount` with two decimals, `date` as yyyy-mm-dd. */
export type Claim = {
  id: number;
  personId: number;
  amount: string;
  date: string;
  note: string;
  status: ClaimStatus;
  /** Given when the claim is opened; null for a DRAFT or VOID claim. */
  number: number | null;
  /** The due fixed when the claim was settled, two decimals; null when it has none, so its due is live. */
  due: string | null;
};

export const claimsKey = (sheet: string) => ['sheet', sheet, 'claims'] as const;

/** Every claim on the sheet, newest first, re-read whenever the sheet is loaded. */
export function useClaims(sheet: string) {
  return useQuery({
    queryKey: claimsKey(sheet),
    queryFn: () => getJson<Claim[]>(`/api/sheets/${encodeURIComponent(sheet)}/claims`),
    staleTime: 0,
    gcTime: 0,
  });
}

/** An amount the server accepts: 0.00 or more, with up to two decimals. */
export const AMOUNT = /^\d+(\.\d{1,2})?$/;

/** The claims that count toward claimed, due-total and every total: OPEN and SETTLED ones. */
export function counted(claims: Claim[]): Claim[] {
  return claims.filter((c) => c.status === 'OPEN' || c.status === 'SETTLED');
}

/** The sum of these claims' amounts, with two decimals. Added up in cents, so it is exact. */
export function claimed(claims: Claim[]): string {
  const cents = claims.reduce((sum, c) => sum + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}

/** What is due on one claim, in cents: the due fixed when it was settled, or else live at `pct`
 *  (null, n/a, when there is no rate). */
export function claimDueCents(claim: Claim, pct: string | undefined): bigint | null {
  return claim.due !== null ? hundredths(claim.due) : dueCents(claim.amount, pct);
}

