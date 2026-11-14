import type { Claim } from '../claims/claims';
import type { Person } from '../people/people';
import { useDueRules } from '../settings/settings';
import { claimDue, useRates } from './rates';

// A claim's due: the one fixed when it was settled, else the greater of the floor and its amount at the rate in force on its date for its person's grade (or its stated figure), plus any late charge on that floored figure while it is overdue.
export function ClaimDue({ claim, person }: { claim: Claim; person: Person }) {
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  return <span data-testid="claim-due">{claimDue(claim, rates, person.grades, rules)}</span>;
}
