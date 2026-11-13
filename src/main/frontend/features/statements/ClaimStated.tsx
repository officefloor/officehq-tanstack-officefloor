import type { Claim } from '../claims/claims';

// Whether the person's last statement bound the claim to its figure.
export function ClaimStated({ claim }: { claim: Claim }) {
  return <span data-testid="claim-stated">{claim.stated ? 'yes' : 'no'}</span>;
}
