import { counts } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { claimDue, total } from '../rates/rates';
import type { Rate } from '../rates/rates';
import type { GradeFrom } from '../people/people';
import type { DueRules } from '../settings/settings';
import { overdue } from './dueBy';

export { overdue } from './dueBy';

/**
 * A person's overdue claims that count (none of their open ones while they are held): how many, and
 * their dues (late charges included) added up (skipping n/a).
 */
export function personOverdue(
  claims: Claim[],
  rates: Rate[],
  grades: GradeFrom[],
  held: boolean,
  rules: DueRules,
): { count: number; total: string } {
  const late = claims.filter((c) => counts(c, held) && overdue(c, rules.today));
  return { count: late.length, total: total(late.map((c) => claimDue(c, rates, grades, rules))) };
}
