import type { Claim } from '../claims/useClaims';

/** The statuses the summary counts; no claim is PENDING today, so its count stays 0. */
export const STATUSES: string[] = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID'];

/** How many of these claims have a status. */
export function countWithStatus(claims: readonly Claim[] | undefined, status: string): number {
  return (claims ?? []).filter((c) => c.status === status).length;
}
