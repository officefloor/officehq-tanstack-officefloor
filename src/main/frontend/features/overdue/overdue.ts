import { type Claim, isOpen } from '../claims/claims';
import { type Late, type Rate, claimDue, dueTotal } from '../rates/rates';
import type { Settings } from '../settings/settings';
import type { Grades } from '../people/people';

/** The claim's due-by (yyyy-MM-dd): fixed when it was added, from the terms of its person's grade then. */
export const dueBy = (claim: Claim): string => claim.dueBy;

/** Whether the claim is overdue: it is OPEN (or PENDING) and its due-by is before the sheet's today. */
export function overdue(claim: Claim, today: string | undefined): boolean {
  return isOpen(claim) && today !== undefined && dueBy(claim) < today;
}

const DAY = 24 * 60 * 60 * 1000;

/** How many full 30 days the claim is overdue on the sheet's today (0 when it is not overdue). */
export function latePeriods(claim: Claim, today: string | undefined): number {
  if (today === undefined || !overdue(claim, today)) return 0;
  const days = (Date.parse(`${today}T00:00:00Z`) - Date.parse(`${dueBy(claim)}T00:00:00Z`)) / DAY;
  return Math.floor(days / 30);
}

/** What the sheet's late charge follows: its late percent, each claim's due-by and its today; and how it rounds:
 *  a claim rounding by a date before the policy date (else the sheet's today) rounds half up. */
export function lateOf(settings: Settings | undefined, today: string | undefined): Late {
  const policy = settings?.policyDate ?? today;
  return {
    pct: settings?.latePct,
    periods: (c) => latePeriods(c, today),
    halfUp: (c) => policy !== undefined && (c.roundsBy ?? c.date) < policy,
  };
}

/** A person's overdue claims among these (their claims that count): how many, and the sum of their
 *  dues (late charge included) at the person's grade in force on each claim's date, skipping n/a. */
export function overdueFigures(
  claims: Claim[],
  rates: Rate[] | undefined,
  grades: Grades,
  floor: string | undefined,
  today: string | undefined,
  lateCharge?: Late,
): { count: number; total: string } {
  const late = claims.filter((c) => overdue(c, today));
  return { count: late.length, total: dueTotal(late.map((c) => claimDue(c, rates, grades, floor, lateCharge))) };
}
