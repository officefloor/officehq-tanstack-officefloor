import type { Claim } from './claims';

// The claim's external reference; empty when it has none.
export function ClaimRef({ claim }: { claim: Claim }) {
  return <span data-testid="claim-ref">{claim.ref ?? ''}</span>;
}
