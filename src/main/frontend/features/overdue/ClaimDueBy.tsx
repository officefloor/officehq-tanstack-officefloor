import type { Claim } from '../claims/claims';
import { useTerms } from '../settings/settings';
import { dueBy } from './overdue';

// A claim's due-by: its date plus the sheet's terms.
export function ClaimDueBy({ claim }: { claim: Claim }) {
  const terms = useTerms();
  return <span data-testid="claim-due-by">{dueBy(claim.date, terms)}</span>;
}
