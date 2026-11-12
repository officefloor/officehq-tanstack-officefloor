import { type Claim, isOpen } from '../claims/claims';
import { type Late, type Rate, claimDue, dueTotal } from '../rates/rates';
import type { Settings } from '../settings/settings';
import type { Grades } from '../people/people';

/** The terms a sheet has until they are set: 14 days. */
export const DEFAULT_TERMS = '14';

/** The claim's due-by (yyyy-MM-dd): its date plus the terms (whole days) in force. */
export function dueBy(claim: Claim, terms: string | undefined): string {
  const d = new Date(`${claim.date}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + Number(terms ?? DEFAULT_TERMS));
  return d.toISOString().slice(0, 10);
}

/** Whether the claim is overdue: it is OPEN (or PENDING) and its due-by is before the sheet's today. */
export function overdue(claim: Claim, terms: string | undefined, today: string | undefined): boolean {
  return isOpen(claim) && today !== undefined && dueBy(claim, terms) < today;
}

const DAY = 24 * 60 * 60 * 1000;

/** How many full 30 days the claim is overdue on the sheet's today (0 when it is not overdue). */
export function latePeriods(claim: Claim, terms: string | undefined, today: string | undefined): number {
  if (today === undefined || !overdue(claim, terms, today)) return 0;
  const days = (Date.parse(`${today}T00:00:00Z`) - Date.parse(`${dueBy(claim, terms)}T00:00:00Z`)) / DAY;
  return Math.floor(days / 30);
}

/** What the sheet's late charge follows: its late percent, its terms and its today; and how it rounds:
 *  a claim rounding by a date before the policy date (else the sheet's today) rounds half up. */
export function lateOf(settings: Settings | undefined, today: string | undefined): Late {
  const policy = settings?.policyDate ?? today;
  return {
    pct: settings?.latePct,
    periods: (c) => latePeriods(c, settings?.terms, today),
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
  terms: string | undefined,
  today: string | undefined,
  lateCharge?: Late,
): { count: number; total: string } {
  const late = claims.filter((c) => overdue(c, terms, today));
  return { count: late.length, total: dueTotal(late.map((c) => claimDue(c, rates, grades, floor, lateCharge))) };
}
