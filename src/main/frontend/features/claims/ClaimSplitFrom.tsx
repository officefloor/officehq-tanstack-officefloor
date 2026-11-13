import type { Claim } from './claims';

// The number of the claim this one was split off; empty for any other claim (the original shows nothing).
export function ClaimSplitFrom({ claim }: { claim: Claim }) {
  return <span data-testid="claim-split-from">{claim.splitFrom ?? ''}</span>;
}
