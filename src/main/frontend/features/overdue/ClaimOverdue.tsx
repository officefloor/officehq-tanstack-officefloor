import type { Claim } from '../claims/claims';
import type { Person } from '../people/people';
import { claimGrace, useDueRules } from '../settings/settings';
import { overdue } from './overdue';

// Whether a claim is overdue: OPEN (or PENDING), with its due-by plus the grace days of its person's grade before the sheet's today.
export function ClaimOverdue({ claim, person }: { claim: Claim; person: Person }) {
  const rules = useDueRules();
  return <span data-testid="claim-overdue">{overdue(claim, rules.today, claimGrace(claim, person.grades, rules)) ? 'yes' : 'no'}</span>;
}
