import type { Claim } from '../claims/claims';
import type { Person } from '../people/people';
import { claimDue, useRates } from './rates';

// A claim's due: the one fixed when it was settled, else its amount at the rate in force on its date for its person's grade.
export function ClaimDue({ claim, person }: { claim: Claim; person: Person }) {
  const rates = useRates().data ?? [];
  return <span data-testid="claim-due">{claimDue(claim, rates, person.grade)}</span>;
}
