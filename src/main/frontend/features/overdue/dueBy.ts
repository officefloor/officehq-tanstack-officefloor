import { deferred, isOpen } from '../claims/claims';
import type { Claim } from '../claims/claims';

/** A date (yyyy-MM-dd) plus whole days. */
function plusDays(date: string, days: number): string {
  return new Date(Date.parse(`${date}T00:00:00Z`) + days * 86_400_000).toISOString().slice(0, 10);
}

/**
 * The date a claim is late after: its due-by, fixed when it was created, plus the grace days of its person's grade in
 * force on its date (0: none).
 */
export function lateFrom(claim: Claim, grace: number): string {
  return grace > 0 ? plusDays(claim.dueBy, grace) : claim.dueBy;
}

/** Whether a claim is overdue: it is OPEN (or PENDING), not deferred, and its due-by plus the grace days is before the sheet's today. */
export function overdue(claim: Claim, today: string, grace = 0): boolean {
  return isOpen(claim) && !deferred(claim, today) && today !== '' && lateFrom(claim, grace) < today;
}

/** How many full 30-day periods a claim is overdue on the sheet's today, counted from its due-by plus the grace days: 0 unless it is overdue. */
export function latePeriods(claim: Claim, today: string, grace = 0): number {
  if (!overdue(claim, today, grace)) return 0;
  const days = (Date.parse(`${today}T00:00:00Z`) - Date.parse(`${lateFrom(claim, grace)}T00:00:00Z`)) / 86_400_000;
  return Math.floor(Math.round(days) / 30);
}

/** The date the claim's nth (1, 2, ...) full 30-day overdue period ends: its due-by plus the grace plus 30 × n days (yyyy-MM-dd). */
export function lateBoundary(claim: Claim, period: number, grace = 0): string {
  return plusDays(lateFrom(claim, grace), period * 30);
}
