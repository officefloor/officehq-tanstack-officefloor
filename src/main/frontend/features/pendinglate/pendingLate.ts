import type { Claim } from '../claims/claims';

/** A date (yyyy-MM-dd) plus whole days. */
function plusDays(date: string, days: number): string {
  return new Date(Date.parse(`${date}T00:00:00Z`) + days * 86_400_000).toISOString().slice(0, 10);
}

/**
 * Whether a claim is late pending: it is PENDING, and its pending day (the sheet's today when it last went to PENDING,
 * from its status log) plus the pending SLA (whole days; '': none, never late) is before the sheet's today.
 */
export function pendingLate(claim: Claim, sla: string, today: string): boolean {
  if (claim.status !== 'PENDING' || sla === '' || today === '') return false;
  const pendingDay = [...(claim.log ?? [])].reverse().find((l) => l.status === 'PENDING')?.date;
  return pendingDay !== undefined && plusDays(pendingDay, Number(sla)) < today;
}
