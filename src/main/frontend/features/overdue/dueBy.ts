import { deferred, isOpen } from '../claims/claims';
import type { Claim } from '../claims/claims';

/** Whether a claim is overdue: it is OPEN (or PENDING), not deferred, and its due-by, fixed when it was created, is before the sheet's today. */
export function overdue(claim: Claim, today: string): boolean {
  return isOpen(claim) && !deferred(claim, today) && today !== '' && claim.dueBy < today;
}

/** How many full 30-day periods a claim is overdue on the sheet's today: 0 unless it is overdue. */
export function latePeriods(claim: Claim, today: string): number {
  if (!overdue(claim, today)) return 0;
  const days = (Date.parse(`${today}T00:00:00Z`) - Date.parse(`${claim.dueBy}T00:00:00Z`)) / 86_400_000;
  return Math.floor(Math.round(days) / 30);
}

/** The date the claim's nth (1, 2, ...) full 30-day overdue period ends: its due-by plus 30 × n days (yyyy-MM-dd). */
export function lateBoundary(claim: Claim, period: number): string {
  return new Date(Date.parse(`${claim.dueBy}T00:00:00Z`) + period * 30 * 86_400_000).toISOString().slice(0, 10);
}
