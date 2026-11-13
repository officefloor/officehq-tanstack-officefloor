import { counts, isOpen } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { claimDue, total } from '../rates/rates';
import type { Rate } from '../rates/rates';
import type { GradeFrom, Hold } from '../people/people';
import type { DueRules } from '../settings/settings';

/** The age buckets a claim falls in by its days on the sheet's today: 0 to 30, 31 to 60, 61 or more. */
export type AgeBucket = '0-30' | '31-60' | '61';

/** A claim's age on the sheet's today, in days from its date; a claim dated after today is 0. */
export function claimAge(claim: Claim, today: string): number {
  if (today === '' || claim.date >= today) return 0;
  return Math.round((Date.parse(`${today}T00:00:00Z`) - Date.parse(`${claim.date}T00:00:00Z`)) / 86_400_000);
}

/** The bucket a claim's age falls in. */
export function ageBucket(claim: Claim, today: string): AgeBucket {
  const age = claimAge(claim, today);
  return age <= 30 ? '0-30' : age <= 60 ? '31-60' : '61';
}

/**
 * A person's OPEN and PENDING claims that count, their dues (late charges included) added up per age
 * bucket (skipping n/a; 0.00 with none).
 */
export function personAged(
  claims: Claim[],
  rates: Rate[],
  grades: GradeFrom[],
  holds: Hold[],
  rules: DueRules,
): Record<AgeBucket, string> {
  const live = claims.filter((c) => isOpen(c) && counts(c, holds, rules.today));
  const sum = (bucket: AgeBucket) =>
    total(live.filter((c) => ageBucket(c, rules.today) === bucket).map((c) => claimDue(c, rates, grades, rules)));
  return { '0-30': sum('0-30'), '31-60': sum('31-60'), '61': sum('61') };
}
