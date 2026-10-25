import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asNumber, useSearchParam } from '../../url/useSearchParam';
import type { Holds } from '../people/people.slot';

export type ClaimStatus = 'DRAFT' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID';

export type Claim = { id: number; personId: number; amount: string; date: string; note: string; status: ClaimStatus;
  /** Given when the claim is opened: the sheet's next (1, 2, ...), or within its period (<period>-<n>, X-<n> in none); none for a draft. */
  number: string | null;
  /** The due fixed when the claim was settled; none while live (not settled, or settled without a rate). */
  settledDue: string | null;
  /** The signed-in user who opened the claim; none when it was never opened. They cannot settle it. */
  openedBy: string | null;
  /** Whether the person's last statement bound the claim: it then shows and totals `statedDue` (none: n/a). */
  stated: boolean;
  statedDue: string | null;
  /** The number of the SETTLED claim this correction (a negative amount) corrects; none for any other claim. */
  corrects: string | null;
  /** The date the claim is due by: fixed when it was added (its date plus its grade's terms then), never moved. */
  dueBy: string;
  /** The claim's status log, oldest first: `<STATUS> by <user> on <date> due=<due>`. */
  log: string[] };

/** Whether a claim counts toward claimed, due and every total: only OPEN, PENDING and SETTLED claims do, and an OPEN
 *  or PENDING one dated within one of its person's holds (`held`: each person's holds) does not unless a statement binds it. */
export function counts(claim: { status: ClaimStatus; personId: number; date: string; stated: boolean }, held: Holds): boolean {
  if (claim.status === 'OPEN' || claim.status === 'PENDING') {
    return claim.stated || !(held.get(claim.personId) ?? []).some((h) =>
      (h.from === null || h.from <= claim.date) && (h.to === null || claim.date <= h.to));
  }
  return claim.status === 'SETTLED';
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
export function sumAmounts(claims: Claim[], held: Holds): string {
  const cents = claims.filter((c) => counts(c, held)).reduce((n, c) => n + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
