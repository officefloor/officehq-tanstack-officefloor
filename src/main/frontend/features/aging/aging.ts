import { isOpen, type Claim } from '../claims/claims';
import { day, type Late } from '../overdue/late';
import { gradeOn, type Person } from '../people/people';
import { claimDue, claimRate, sum, type Rate } from '../rates/rates';
import type { Dated } from '../settings/settings';

/** A person's due split by age: the dues (late charge included, skipping n/a) of their open (OPEN or PENDING) claims
 *  that count, by the days from the claim's date to the sheet's today — 0 to 30, 31 to 60, 61 or more (a claim dated
 *  after today is 0 to 30). */
export function aged(
  claims: Claim[],
  rates: Rate[] | undefined,
  person: Person,
  late: Late,
  floor: Dated,
): { d30: string; d60: string; d61: string } {
  const buckets: [string[], string[], string[]] = [[], [], []];
  for (const c of claims) {
    if (!isOpen(c) || c.deferred) continue;
    const age = late.today === '' ? 0 : day(late.today) - day(c.date);
    const figure = claimDue(c, claimRate(rates, gradeOn(person, c.date), c), floor, late);
    buckets[age <= 30 ? 0 : age <= 60 ? 1 : 2].push(figure);
  }
  return { d30: sum(buckets[0]), d60: sum(buckets[1]), d61: sum(buckets[2]) };
}
