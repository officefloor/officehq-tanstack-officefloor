import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asNumber, useSearchParam } from '../../url/useSearchParam';

export type ClaimStatus = 'DRAFT' | 'OPEN' | 'SETTLED' | 'VOID';

export type Claim = { id: number; personId: number; amount: string; date: string; note: string; status: ClaimStatus;
  /** Given when the claim is opened; none for a draft or a void claim. */
  number: number | null;
  /** The due fixed when the claim was settled; none while live (not settled, or settled without a rate). */
  settledDue: string | null;
  /** The signed-in user who opened the claim; none when it was never opened. They cannot settle it. */
  openedBy: string | null;
  /** Whether the person's last statement bound the claim: it then shows and totals `statedDue` (none: n/a). */
  stated: boolean;
  statedDue: string | null };

/** Whether a claim counts toward claimed, due and every total: only OPEN and SETTLED claims do. */
export function counts(claim: { status: ClaimStatus }): boolean {
  return claim.status === 'OPEN' || claim.status === 'SETTLED';
}

/** The key every feature reads the sheet's claims under. */
export const claimsKey = (sheetId: string) => ['sheet', sheetId, 'claims'] as const;

/** Every claim on the sheet, newest first. */
export function useClaims(sheetId: string) {
  return useQuery({
    queryKey: claimsKey(sheetId),
    queryFn: () => getJson<Claim[]>(`/api/sheets/${encodeURIComponent(sheetId)}/claims`),
  });
}

/** Which claim's editor is open (search param `editClaim`); none means each claim form adds. */
export function useEditingClaim() {
  return useSearchParam('editClaim', asNumber);
}

/** The counted claims' amounts added up exactly, in cents, shown with two decimals. */
export function sumAmounts(claims: Claim[]): string {
  const cents = claims.filter(counts).reduce((n, c) => n + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
