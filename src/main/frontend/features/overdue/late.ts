import { isOpen, type Claim } from '../claims/claims';

/** What the late charge on a sheet depends on: its today (yyyy-MM-dd, '' while it loads), its
 *  late percent in force on a date (two decimals; a claim's charge takes the one in force on each of its 30-day boundaries) and its policy date (yyyy-MM-dd; its today while blank): a claim whose `roundsOn` is
 *  before it rounds its live figures half up, one on or after it half to even. */
export type Late = { today: string; pct: (date: string) => string; policy: string };

/** Whether the claim's live figures round half up (it rounds on a date before the policy date) rather than half to even. */
export function roundsHalfUp(claim: Claim, late: Late | undefined): boolean {
  return late !== undefined && late.policy !== '' && claim.roundsOn < late.policy;
}

/** Days since 1970-01-01 of a yyyy-MM-dd date. */
export function day(date: string): number {
  const [y, m, d] = date.split('-').map(Number);
  return Date.UTC(y, m - 1, d) / 86_400_000;
}

/** A claim's due-by (yyyy-MM-dd): fixed when it was added, its date plus its person's own terms then, else those of their grade. */
export function dueBy(claim: Claim): string {
  return claim.dueBy;
}

/** Whether a claim is overdue: it is open (OPEN or PENDING), not deferred, and its due-by is before the sheet's today. */
export function isOverdue(claim: Claim, today: string): boolean {
  return isOpen(claim) && !claim.deferred && today !== '' && dueBy(claim) < today;
}

/** How many full 30 days an overdue claim is overdue (0 when it is not overdue, its due is fixed by settling, or it is a
 *  correction, which bears no late charge). */
export function lateSteps(claim: Claim, late: Late): number {
  if (claim.due !== null || claim.amount.trim().startsWith('-') || !isOverdue(claim, late.today)) return 0;
  return Math.floor((day(late.today) - day(dueBy(claim))) / 30);
}

/** A yyyy-MM-dd date the given days after another. */
export function plusDays(date: string, days: number): string {
  return new Date((day(date) + days) * 86_400_000).toISOString().slice(0, 10);
}

/** The 30-day boundaries an overdue claim has passed (its due-by plus 30, 60, 90 ... days, on or before today), each
 *  bearing the late percent in force on it; none when it bears no late charge (see lateSteps). */
export function lateBoundaries(claim: Claim, late: Late): string[] {
  const steps = lateSteps(claim, late);
  return Array.from({ length: steps }, (_, i) => plusDays(dueBy(claim), 30 * (i + 1)));
}
