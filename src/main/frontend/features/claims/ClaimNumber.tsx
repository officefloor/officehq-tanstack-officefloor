import type { Claim } from './claims';

// A claim's number on the sheet; empty while it is a draft and once it is voided.
export function ClaimNumber({ claim }: { claim: Claim }) {
  return <span data-testid="claim-number">{claim.number ?? ''}</span>;
}
