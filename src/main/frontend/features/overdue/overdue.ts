import { gradeOn } from '../people/people';
import type { GradeFrom } from '../people/people';
import { counts, isOpen } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { claimDue, rateOn, sumDue } from '../rates/rates';
import type { Late, Rate } from '../rates/rates';

/** A date (yyyy-MM-dd) and a number of days after it, as yyyy-MM-dd. Worked in UTC so no zone shifts it. */
export function addDays(date: string, days: number): string {
  const [y, m, d] = date.split('-').map(Number);
  return new Date(Date.UTC(y, m - 1, d + days)).toISOString().slice(0, 10);
}

/** A claim's due-by: its date plus the sheet's terms (days). */
export const dueBy = (claim: Claim, terms: number): string => addDays(claim.date, terms);

/** Whether a claim is overdue: open (OPEN or PENDING), and its due-by before the sheet's `today`. */
export const isOverdue = (claim: Claim, terms: number, today: string): boolean =>
  isOpen(claim) && today !== '' && dueBy(claim, terms) < today;

/**
 * A person's overdue claims that count (none while they are `held`, as an OPEN claim then counts in
 * no total): how many, and their due figures (with their late charge) added up, skipping n/a.
 */
export function personOverdue(
  rates: Rate[],
  grades: GradeFrom[],
  today: string,
  terms: number,
  claims: Claim[],
  held: boolean,
  floor: string,
  late: Late,
): { count: number; total: string } {
  const overdue = claims.filter((c) => counts(c, held) && isOverdue(c, terms, today));
  return {
    count: overdue.length,
    total: sumDue(overdue.map((c) => claimDue(c, rateOn(rates, gradeOn(grades, c.date), c.date), floor, late))),
  };
}
