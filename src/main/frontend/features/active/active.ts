import type { Hold } from '../people/people';
import { counts, isOpen } from '../claims/claims';
import type { Claim } from '../claims/claims';
import { addDays } from '../rates/rates';

/** How many days back from the sheet's today (today included) a claim keeps its person active. */
export const ACTIVE_DAYS = 30;

/**
 * Whether a person is active: they have an open (OPEN or PENDING) claim that counts, dated within
 * the 30 days up to and including the sheet's `today`.
 */
export function isActive(claims: Claim[], holds: Hold[], today: string): boolean {
  if (today === '') return false;
  const since = addDays(today, -(ACTIVE_DAYS - 1));
  return claims.some((c) => isOpen(c) && counts(c, holds, today) && c.date >= since && c.date <= today);
}
