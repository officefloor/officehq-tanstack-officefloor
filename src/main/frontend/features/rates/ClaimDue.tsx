import type { Claim } from '../claims/claims';
import type { Person } from '../people/people';
import { due, rateFor, useRates } from './rates';

// A claim's due: its amount at the rate for its person's grade.
export function ClaimDue({ claim, person }: { claim: Claim; person: Person }) {
  const rates = useRates().data ?? [];
  return <span data-testid="claim-due">{due(claim.amount, rateFor(rates, person.grade))}</span>;
}
