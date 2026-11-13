import type { Claim } from './claims';

// The user who opened the claim; empty while it is a draft.
export function ClaimOpenedBy({ claim }: { claim: Claim }) {
  return <span data-testid="claim-opened-by">{claim.openedBy ?? ''}</span>;
}
