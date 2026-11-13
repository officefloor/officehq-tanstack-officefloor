import type { Claim } from '../claims/claims';
import { periodOf, usePeriods } from './periods';

// The period a claim belongs to: the one whose dates contain its date; blank with none.
export function ClaimPeriod({ claim }: { claim: Claim }) {
  const periods = usePeriods().data ?? [];
  return <span data-testid="claim-period">{periodOf(periods, claim.date)?.name ?? ''}</span>;
}
