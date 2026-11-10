import type { Grade } from '../people/people';
import { counts } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { claimDue, rateOn, sumDue } from '../rates/rates';
import type { Rate } from '../rates/rates';

/** A date (yyyy-MM-dd) and a number of days after it, as yyyy-MM-dd. Worked in UTC so no zone shifts it. */
export function addDays(date: string, days: number): string {
  const [y, m, d] = date.split('-').map(Number);
  return new Date(Date.UTC(y, m - 1, d + days)).toISOString().slice(0, 10);
}

/** A claim's due-by: its date plus the sheet's terms (days). */
export const dueBy = (claim: Claim, terms: number): string => addDays(claim.date, terms);

/** Whether a claim is overdue: OPEN, and its due-by before the sheet's `today`. */
export const isOverdue = (claim: Claim, terms: number, today: string): boolean =>
  claim.status === 'OPEN' && today !== '' && dueBy(claim, terms) < today;

/**
 * A person's overdue claims that count (none while they are `held`, as an OPEN claim then counts in
 * no total): how many, and their due figures added up, skipping n/a.
 */
export function personOverdue(
  rates: Rate[],
  grade: Grade,
  today: string,
  terms: number,
  claims: Claim[],
  held: boolean,
  floor: string,
): { count: number; total: string } {
  const overdue = claims.filter((c) => counts(c, held) && isOverdue(c, terms, today));
  return {
    count: overdue.length,
    total: sumDue(overdue.map((c) => claimDue(c, rateOn(rates, grade, c.date), floor))),
  };
}
