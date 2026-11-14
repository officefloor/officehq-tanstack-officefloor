import type { Claim } from '../claims/claims';

/** A date (yyyy-MM-dd) plus whole days. */
function plusDays(date: string, days: number): string {
  return new Date(Date.parse(`${date}T00:00:00Z`) + days * 86_400_000).toISOString().slice(0, 10);
}

/**
 * Whether a claim is late in review: it is REVIEW, and its review day (the sheet's today when it last went to REVIEW,
 * from its status log) plus the review SLA (whole days; '': none, never late) is before the sheet's today.
 */
export function reviewLate(claim: Claim, sla: string, today: string): boolean {
  if (claim.status !== 'REVIEW' || sla === '' || today === '') return false;
  const reviewDay = [...(claim.log ?? [])].reverse().find((l) => l.status === 'REVIEW')?.date;
  return reviewDay !== undefined && plusDays(reviewDay, Number(sla)) < today;
}
