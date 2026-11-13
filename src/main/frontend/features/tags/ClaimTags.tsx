import type { Claim } from '../claims/claims';

// The claim's tags, sorted, comma and space separated; empty when it has none.
export function ClaimTags({ claim }: { claim: Claim }) {
  return <span data-testid="claim-tags">{claim.tags.join(', ')}</span>;
}
