import { gradeOn } from '../people/people';
import type { GradeFrom } from '../people/people';
import { counts, isOpen } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { claimDue, rateOn, sumDue } from '../rates/rates';
import type { Late, Rate } from '../rates/rates';

/** A claim's due-by: fixed when it was made, from its grade's terms then. */
export const dueBy = (claim: Claim): string => claim.dueBy;

/** Whether a claim is overdue: open (OPEN or PENDING), and its due-by before the sheet's `today`. */
export const isOverdue = (claim: Claim, today: string): boolean =>
  isOpen(claim) && today !== '' && dueBy(claim) < today;

/**
 * A person's overdue claims that count (none while they are `held`, as an OPEN claim then counts in
 * no total): how many, and their due figures (with their late charge) added up, skipping n/a.
 */
export function personOverdue(
  rates: Rate[],
  grades: GradeFrom[],
  today: string,
  claims: Claim[],
  held: boolean,
  floor: string,
  late: Late,
): { count: number; total: string } {
  const overdue = claims.filter((c) => counts(c, held) && isOverdue(c, today));
  return {
    count: overdue.length,
    total: sumDue(overdue.map((c) => claimDue(c, rateOn(rates, gradeOn(grades, c.date), c.date), floor, late))),
  };
}
