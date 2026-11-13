import type { Claim } from '../claims/claims';
import type { Person } from '../people/people';
import { useDueRules } from '../settings/settings';
import { claimDue, useRates } from './rates';

// A claim's due: the one fixed when it was settled, else its amount at the rate in force on its date for its person's grade, at least the floor, plus any late charge while it is overdue.
export function ClaimDue({ claim, person }: { claim: Claim; person: Person }) {
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  return <span data-testid="claim-due">{claimDue(claim, rates, person.grade, rules)}</span>;
}
