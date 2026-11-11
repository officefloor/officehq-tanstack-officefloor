import { gradeOn } from '../people/people';
import type { GradeFrom, Hold } from '../people/people';
import { counts } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { claimDue, NA, rateOn } from '../rates/rates';
import type { Dated, Late, Rate } from '../rates/rates';
import { personOverdue } from '../overdue/overdue';

/** A two-decimal figure as a whole count of hundredths. */
function hundredths(value: string): bigint {
  const negative = value.startsWith('-');
  const [whole, frac = ''] = (negative ? value.slice(1) : value).split('.');
  const cents = BigInt(whole || '0') * 100n + BigInt((frac + '00').slice(0, 2));
  return negative ? -cents : cents;
}

/** `n / d` (d > 0) rounded half to even. */
function divHalfEven(n: bigint, d: bigint): bigint {
  let q = n / d;
  let r = n % d;
  if (r < 0n) {
    q -= 1n;
    r += d;
  }
  if (2n * r > d || (2n * r === d && q % 2n !== 0n)) q += 1n;
  return q;
}

/**
 * A person's score: the due of their SETTLED claims that count (skipping n/a) divided by 100, minus 5
 * for every overdue claim of theirs that counts, rounded half to even to a whole number.
 */
export function personScore(
  rates: Rate[],
  grades: GradeFrom[],
  today: string,
  claims: Claim[],
  holds: Hold[],
  floor: Dated,
  late: Late,
): string {
  const settled = claims
    .filter((c) => c.status === 'SETTLED' && counts(c, holds, today))
    .map((c) => claimDue(c, rateOn(rates, gradeOn(grades, c.date), c.date, c.id), floor, late, gradeOn(grades, c.date)))
    .filter((d) => d !== NA)
    .reduce((n, d) => n + hundredths(d), 0n);
  const { count } = personOverdue(rates, grades, today, claims, holds, floor, late);
  // In ten-thousandths: settled cents / 100 is settled / 10000 units of 1, and each overdue claim is 5.
  return divHalfEven(settled - 50000n * BigInt(count), 10000n).toString();
}
