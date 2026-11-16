import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { heldOn, type Person } from '../people/people';

/** A claim's status: DRAFT when added, then OPEN and SETTLED; a clerk settling above the clerk limit makes it PENDING
 *  until a steward approves (SETTLED) or rejects (OPEN) it; a DRAFT, OPEN or PENDING claim may be VOID, as may a SETTLED one
 *  by a steward within 7 days of its settlement. */
export type ClaimStatus = 'DRAFT' | 'OPEN' | 'PENDING' | 'SETTLED' | 'VOID';

/** A claim of a person. `amount` is text with two decimals (never a float); `date` is yyyy-MM-dd; `number` (text: 1, 2, ..., or P1-1, X-1 when numbered per period) is given
 *  when the claim is opened (null for a draft or a voided claim); `due` is the due fixed when it was settled (null
 *  while its due is live, worked out from its grade's current rate); `openedBy` is who opened it (null when never
 *  opened); `stated` is whether its person's last statement bound it to the figure `statedDue` (null when it had
 *  none), which it shows while its due is not fixed; `corrects` is, for a correction (a negative amount),
 *  the number of the SETTLED claim of the same person it corrects (null for any other claim); `roundsOn` (yyyy-MM-dd)
 *  is the date that decides how its live figures round: the corrected claim's date for a correction, else its own;
 *  `dueBy` (yyyy-MM-dd) is its due-by, fixed when it was added from its person's own terms, else those of their grade, on its date;
 *  `deferred` is whether it is deferred on the sheet's today (an OPEN claim deferred until a date after it: it counts in no
 *  total, is not overdue and cannot be settled) and `deferredUntil` (yyyy-MM-dd) the date it was last deferred until (shown
 *  even once that date has passed; null when never deferred); `proofs` is its proofs count (a claim whose due is above the sheet's proof threshold
 *  on its date cannot be settled nor approved while it is 0); `splitFrom` is the number of the claim it was split from
 *  (null for any other claim); `log` is its status log, oldest first; `lockedBy` is who has its editor open (it is locked for
 *  everyone else; null when no one does). */
export type Claim = {
  id: number;
  sheetId: string;
  personId: number;
  amount: string;
  date: string;
  note: string;
  status: ClaimStatus;
  number: string | null;
  due: string | null;
  openedBy: string | null;
  stated: boolean;
  statedDue: string | null;
  corrects: string | null;
  roundsOn: string;
  dueBy: string;
  deferred: boolean;
  deferredUntil: string | null;
  proofs: number;
  splitFrom: string | null;
  log: ClaimLogLine[];
  lockedBy: string | null;
};

/** One line of a claim's status log: the status it took, who made the change, the sheet's today (yyyy-MM-dd) when
 *  it was made, and the claim's due as shown at that moment (two decimals, or n/a). */
export type ClaimLogLine = { status: ClaimStatus; by: string; date: string; due: string };

/** A status log line as shown: `<STATUS> by <user> on <date> due=<due>`. */
export const logLine = (l: ClaimLogLine) => `${l.status} by ${l.by} on ${l.date} due=${l.due}`;

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

/** Whether a claim is open: OPEN, or PENDING (handled like an OPEN claim but not bound by a statement). */
export const isOpen = (c: Claim) => c.status === 'OPEN' || c.status === 'PENDING';

/** Whether a claim counts toward claimed and the totals: only OPEN, PENDING and SETTLED claims do, and no deferred one. */
export const counts = (c: Claim) => (isOpen(c) || c.status === 'SETTLED') && !c.deferred;

/** Whether a statement binds the claim now: it is stated and not PENDING. */
export const bound = (c: Claim) => c.stated && c.status !== 'PENDING';

/** The claims that count in the live totals (claimed, due-totals, a period's due-total): a person's open (OPEN or
 *  PENDING) claims dated within a hold of theirs, and not bound by a statement, do not; their other claims still do. */
export function live(claims: Claim[], people: Person[] | undefined): Claim[] {
  const byId = new Map((people ?? []).map((p) => [p.id, p]));
  return claims.filter((c) => {
    const p = byId.get(c.personId);
    return counts(c) && !(isOpen(c) && !bound(c) && p !== undefined && heldOn(p, c.date));
  });
}

/** The amounts of the claims that count added up, shown with two decimals. */
export function total(claims: Claim[]): string {
  return (claims.filter(counts).reduce((sum, c) => sum + cents(c.amount), 0) / 100).toFixed(2);
}
