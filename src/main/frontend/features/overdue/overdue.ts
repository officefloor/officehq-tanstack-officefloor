import { gradeOn } from '../people/people';
import type { GradeFrom, Hold } from '../people/people';
import { counts, isDeferred, isOpen } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { claimDue, overdueFrom, rateOn, sumDue } from '../rates/rates';
import type { Dated, Late, Rate } from '../rates/rates';

/** A claim's due-by: fixed when it was made, the terms-th business day after its date, from its person's own terms, else its grade's terms then. */
export const dueBy = (claim: Claim): string => claim.dueBy;

/**
 * Whether a claim is overdue: open (OPEN or PENDING), not deferred, and its due-by plus the grace of
 * its `grade` (the one in force on its date) before the sheet's `today`.
 */
export const isOverdue = (claim: Claim, today: string, late: Late, grade: string): boolean =>
  isOpen(claim) && !isDeferred(claim, today) && today !== '' && overdueFrom(claim, grade, late) < today;

/**
 * A person's overdue claims that count (not an unbound OPEN one dated within one of their `holds`, as it then counts in
 * no total): how many, and their due figures (with their late charge) added up, skipping n/a.
 */
export function personOverdue(
  rates: Rate[],
  grades: GradeFrom[],
  today: string,
  claims: Claim[],
  holds: Hold[],
  floor: Dated,
  late: Late,
): { count: number; total: string } {
  const overdue = claims.filter((c) => counts(c, holds, today) && isOverdue(c, today, late, gradeOn(grades, c.date)));
  return {
    count: overdue.length,
    total: sumDue(overdue.map((c) => claimDue(c, rateOn(rates, gradeOn(grades, c.date), c.date), floor, late, gradeOn(grades, c.date)))),
  };
}
