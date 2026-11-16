import { isOpen, type Claim } from '../claims/claims';

/** What the late charge on a sheet depends on: its today (yyyy-MM-dd, '' while it loads), its
 *  late percent in force on a date (two decimals; a claim's charge takes the one in force on each of its 30-day boundaries) and its policy date (yyyy-MM-dd; its today while blank): a claim whose `roundsOn` is
 *  before it rounds its live figures half up, one on or after it half to even; and a claim's grace in days (that of its
 *  person's grade in force on its date, in force on that date; 0 when none); and the rounding mode in force on a date
 *  (half-up or half-even; null when none), which, when a claim's `roundsOn` has one, decides how it rounds instead. */
export type Late = {
  today: string; pct: (date: string) => string; policy: string; grace: (claim: Claim) => number;
  rounding: (date: string) => string | null;
};

/** Whether the claim's live figures round half up (by the rounding mode in force on its `roundsOn`, else when it rounds
 *  on a date before the policy date) rather than half to even. */
export function roundsHalfUp(claim: Claim, late: Late | undefined): boolean {
  if (late === undefined) return false;
  const mode = late.rounding(claim.roundsOn);
  if (mode === 'half-up') return true;
  if (mode === 'half-even') return false;
  return late.policy !== '' && claim.roundsOn < late.policy;
}

/** Days since 1970-01-01 of a yyyy-MM-dd date. */
export function day(date: string): number {
  const [y, m, d] = date.split('-').map(Number);
  return Date.UTC(y, m - 1, d) / 86_400_000;
}

/** A claim's due-by (yyyy-MM-dd): fixed when it was added, the terms-th business day after its date (skipping weekends and the sheet's holidays then), by its person's own terms then, else those of their grade. */
export function dueBy(claim: Claim): string {
  return claim.dueBy;
}

/** The date a claim is late from (yyyy-MM-dd): its due-by plus its grace (see Late). */
export function lateFrom(claim: Claim, late: Late): string {
  const grace = late.grace(claim);
  return grace === 0 ? dueBy(claim) : plusDays(dueBy(claim), grace);
}

/** Whether a claim is overdue: it is open (OPEN or PENDING), not deferred, and its due-by plus its grace is before the sheet's today. */
export function isOverdue(claim: Claim, late: Late): boolean {
  return isOpen(claim) && !claim.deferred && late.today !== '' && lateFrom(claim, late) < late.today;
}

/** How many full 30 days an overdue claim is overdue (0 when it is not overdue, its due is fixed by settling, or it is a
 *  correction, which bears no late charge). */
export function lateSteps(claim: Claim, late: Late): number {
  if (claim.due !== null || claim.amount.trim().startsWith('-') || !isOverdue(claim, late)) return 0;
  return Math.floor((day(late.today) - day(lateFrom(claim, late))) / 30);
}

/** A yyyy-MM-dd date the given days after another. */
export function plusDays(date: string, days: number): string {
  return new Date((day(date) + days) * 86_400_000).toISOString().slice(0, 10);
}

/** Whether a REVIEW claim is review-late: its review day (the sheet's today on the latest REVIEW line of its status log)
 *  plus the review SLA days is before the sheet's today (never with no SLA, or for a claim not in REVIEW). */
export function isReviewLate(claim: Claim, sla: number | null, today: string): boolean {
  if (claim.status !== 'REVIEW' || sla === null || today === '') return false;
  const reviewed = [...claim.log].reverse().find((l) => l.status === 'REVIEW');
  return reviewed !== undefined && plusDays(reviewed.date, sla) < today;
}

/** The 30-day boundaries an overdue claim has passed (its due-by plus its grace plus 30, 60, 90 ... days, on or before today), each
 *  bearing the late percent in force on it; none when it bears no late charge (see lateSteps). */
export function lateBoundaries(claim: Claim, late: Late): string[] {
  const steps = lateSteps(claim, late);
  return Array.from({ length: steps }, (_, i) => plusDays(lateFrom(claim, late), 30 * (i + 1)));
}
