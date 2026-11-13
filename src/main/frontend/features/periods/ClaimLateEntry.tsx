import type { Claim } from '../claims/claims';

// Whether the claim is a late entry: added while the period containing its date was soft-closed.
export function ClaimLateEntry({ claim }: { claim: Claim }) {
  return <span data-testid="claim-late-entry">{claim.lateEntry ? 'yes' : 'no'}</span>;
}
