import { gradeOn } from '../people/people';
import type { GradeFrom, Hold } from '../people/people';
import { counts, isOpen } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { claimDue, daysBetween, rateOn, sumDue } from '../rates/rates';
import type { Dated, Late, Rate } from '../rates/rates';

/** A person's due split by claim age: aged 0 to 30 days, 31 to 60, and 61 or more. */
export type Aged = { upTo30: string; upTo60: string; over60: string };

/**
 * A person's open (OPEN or PENDING) claims that count (not an unbound one dated within one of their
 * `holds`, nor one deferred on `today`), their due figures (with their late charge) added up, skipping
 * n/a, split by the claim's age on the sheet's `today`: the days from its date. A claim dated after
 * today is aged 0 to 30.
 */
export function personAged(
  rates: Rate[],
  grades: GradeFrom[],
  today: string,
  claims: Claim[],
  holds: Hold[],
  floor: Dated,
  late: Late,
): Aged {
  const open = claims.filter((c) => isOpen(c) && counts(c, holds, today));
  const age = (c: Claim) => (today === '' ? 0 : daysBetween(c.date, today));
  const sum = (bucket: Claim[]) =>
    sumDue(bucket.map((c) => claimDue(c, rateOn(rates, gradeOn(grades, c.date), c.date, c.id), floor, late, gradeOn(grades, c.date))));
  return {
    upTo30: sum(open.filter((c) => age(c) <= 30)),
    upTo60: sum(open.filter((c) => age(c) > 30 && age(c) <= 60)),
    over60: sum(open.filter((c) => age(c) > 60)),
  };
}
