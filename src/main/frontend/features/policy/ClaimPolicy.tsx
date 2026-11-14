import type { Claim } from '../claims/claims';
import { useSettings } from '../settings/settings';
import { policyVersionOn } from './policy';

// The policy version in force on a claim's date.
export function ClaimPolicy({ claim }: { claim: Claim }) {
  const settings = useSettings().data;
  return <span data-testid="claim-policy">{settings ? policyVersionOn(settings.policyVersions, claim.date) : ''}</span>;
}
