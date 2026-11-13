import { counts, isOpen } from '../claims/claims';
import type { Claim } from '../claims/claims';
import type { Hold } from '../people/people';

/** How many days back from the sheet's today, today included, a claim keeps its person active. */
export const ACTIVE_DAYS = 30;

/** The first date (yyyy-MM-dd) of the active window ending on the sheet's today. */
const windowStart = (today: string) =>
  new Date(Date.parse(`${today}T00:00:00Z`) - (ACTIVE_DAYS - 1) * 86_400_000).toISOString().slice(0, 10);

/**
 * Whether a person is active: they have an OPEN or PENDING claim that counts (not one a hold covers, nor a
 * deferred one) dated within the 30 days up to and including the sheet's today. `claims` are theirs.
 */
export function isActive(claims: Claim[], holds: Hold[], today: string): boolean {
  if (today === '') return false;
  const from = windowStart(today);
  return claims.some((c) => isOpen(c) && counts(c, holds, today) && c.date >= from && c.date <= today);
}
