import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asNumber, useSearchParam } from '../../url/useSearchParam';
import type { Holds } from '../people/people.slot';

export type ClaimStatus = 'DRAFT' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID' | 'DELETED' | 'REVIEW';

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
  /** The date the claim is due by: the terms-th business day after its date when added (skipping Saturdays, Sundays and the sheet's holidays now), by the terms fixed then. */
  dueBy: string;
  /** The claim's status log, oldest first: `<STATUS> by <user> on <date> due=<due>`. */
  log: string[];
  /** Whether the OPEN claim is deferred past the sheet's today: it then counts in no total, is not overdue and cannot be settled. */
  deferred: boolean;
  /** The date it was last deferred until; none when it was never deferred. */
  deferredUntil: string | null;
  /** How many proofs the claim carries; one above the proof threshold needs one before it is settled or approved. */
  proofs: number;
  /** How many of its proofs are receipts (a proof with no kind chosen is one). */
  proofsReceipt: number;
  /** How many of its proofs are photos. */
  proofsPhoto: number;
  /** The number of the claim this one was split off; none when it was not split off one. */
  splitFrom: string | null;
  /** Whether it was added while the period containing its due-by was soft-closed. */
  lateEntry: boolean;
  /** The last sheet-today a reminder was written for the claim; none when it was never reminded. */
  reminded: string | null;
  /** Whether a steward waived its late charge: it bears none from then on. */
  waived: boolean;
  /** Whether the claim is held: OPEN or PENDING, it then counts in no total unless a statement binds it. */
  held: boolean;
  /** The claim's external reference, used at most once on the sheet (case aside); none when it has none. */
  ref: string | null;
  /** The claim's tags, sorted; empty when it has none. */
  tags: string[];
  /** The reason the claim was voided with, from the sheet's list; none when none was given. */
  voidReason: string | null;
  /** The claim's comments, oldest first: `<user> on <date>: <text>` (the sheet's today when written). */
  comments: string[];
  /** The claim's lines, in the order added: `<label> <qty> x <unit>`; with lines, its amount is their total. */
  lines: string[];
  /** The number of the bulk settle batch that settled the claim; none when no batch settled it. */
  settleBatch: number | null;
  /** Where the claim's adding stands in the order of changes: a forward-only rate applies to it only when saved before
   *  it (see ratesForClaim); none for a claim added before every forward-only rate. */
  added: number | null;
  /** The sheet's today when the REVIEW claim went to REVIEW; none for a claim in any other status. */
  reviewDay?: string | null };

/** Whether a claim counts toward claimed, due and every total: only OPEN, PENDING and SETTLED claims do, and an OPEN
 *  or PENDING one held itself or dated within one of its person's holds (`held`: each person's holds) does not unless a statement binds it;
 *  nor does one deferred past the sheet's today. */
export function counts(claim: { status: ClaimStatus; personId: number; date: string; stated: boolean; deferred?: boolean; held?: boolean }, held: Holds): boolean {
  if (claim.deferred) return false;
  if (claim.status === 'OPEN' || claim.status === 'PENDING') {
    return claim.stated || (!claim.held && !(held.get(claim.personId) ?? []).some((h) =>
      (h.from === null || h.from <= claim.date) && (h.to === null || claim.date <= h.to)));
  }
  return claim.status === 'SETTLED';
}

/** The key every feature reads the sheet's claims under. */
export const claimsKey = (sheetId: string) => ['sheet', sheetId, 'claims'] as const;

/** The key of a claim editor's lock request: a claim form waits for it before it submits. */
export const claimLockKey = (sheetId: string) => ['sheet', sheetId, 'claim-lock'] as const;

/** The key of a claim form's save: saving a claim's editor clears the claim's lock refusal. */
export const claimSaveKey = (sheetId: string) => ['sheet', sheetId, 'claim-save'] as const;

/** Every claim on the sheet, newest first. */
export function useClaims(sheetId: string) {
  return useQuery({
    queryKey: claimsKey(sheetId),
    queryFn: () => getJson<Claim[]>(`/api/sheets/${encodeURIComponent(sheetId)}/claims`),
  });
}

/** Every tag ever used on the sheet's claims or people, sorted; under the claims' key, so whatever refreshes the claims refreshes it. */
export function useClaimTags(sheetId: string) {
  return useQuery({
    queryKey: [...claimsKey(sheetId), 'tags'] as const,
    queryFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/tags`),
  });
}

/** The number of the sheet's last bulk settle batch (none when there has been none); under the claims' key, so
 *  whatever refreshes the claims refreshes it. */
export function useLastSettleBatch(sheetId: string) {
  return useQuery({
    queryKey: [...claimsKey(sheetId), 'settle-batch'] as const,
    queryFn: () => getJson<{ batch: number | null }>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/settle-batch`),
  });
}

/** Which claim's editor is open (search param `editClaim`); none means each claim form adds. */
export function useEditingClaim() {
  return useSearchParam('editClaim', asNumber);
}

/** The counted claims' amounts, corrections left out, added up exactly, in cents, shown with two decimals. */
export function sumAmounts(claims: Claim[], held: Holds): string {
  const cents = claims.filter((c) => counts(c, held) && c.corrects == null).reduce((n, c) => n + Math.round(Number(c.amount) * 100), 0);
  return (cents / 100).toFixed(2);
}
