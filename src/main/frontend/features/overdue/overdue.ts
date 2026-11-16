import type { Claim } from '../claims/claims';
import { gradeOn, type Person } from '../people/people';
import { claimDue, rateFor, sum, type Rate } from '../rates/rates';
import type { Dated } from '../settings/settings';
import { isOverdue, type Late } from './late';

export { dueBy, isOverdue } from './late';

/** The overdue count and overdue-total (their dues, late charge included, added up, skipping n/a) of the given
 *  claims that count. */
export function overdue(
  claims: Claim[],
  rates: Rate[] | undefined,
  person: Person,
  late: Late,
  floor: Dated,
): { count: number; total: string } {
  const over = claims.filter((c) => isOverdue(c, late));
  return { count: over.length, total: sum(over.map((c) => claimDue(c, rateFor(rates, gradeOn(person, c.date), c.date), floor, late))) };
}
