import type { Claim } from './claims';

// The number of the bulk settle batch that settled the claim; empty when it was settled one at a time or never.
export function ClaimBatch({ claim }: { claim: Claim }) {
  return <span data-testid="claim-batch">{claim.batch ?? ''}</span>;
}
