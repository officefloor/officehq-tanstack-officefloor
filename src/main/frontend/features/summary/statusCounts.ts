import type { Claim } from '../claims/useClaims';

/** The statuses the summary counts. */
export const STATUSES: string[] = ['DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID', 'REVIEW'];

/** How many of these claims have a status. */
export function countWithStatus(claims: readonly Claim[] | undefined, status: string): number {
  return (claims ?? []).filter((c) => c.status === status).length;
}
