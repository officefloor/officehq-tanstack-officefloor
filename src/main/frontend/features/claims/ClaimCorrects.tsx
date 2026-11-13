import type { Claim } from './claims';

// The number of the claim a correction corrects; empty for any other claim.
export function ClaimCorrects({ claim }: { claim: Claim }) {
  return <span data-testid="claim-corrects">{claim.corrects ?? ''}</span>;
}
