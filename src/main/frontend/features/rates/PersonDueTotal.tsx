import { useClaims } from '../claims/claims';
import type { Person } from '../people/people';
import { useDueRules } from '../settings/settings';
import { personDue, useRates } from './rates';

// A person's due-total: the figures of their claims that count, added up; n/a when their grade has no
// rate on the sheet's today, unless a settled or stated claim keeps a figure.
export function PersonDueTotal({ person }: { person: Person }) {
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  const claims = (useClaims().data ?? []).filter((c) => c.personId === person.id);
  return <td data-testid="person-due-total">{personDue(claims, rates, person.grades, person.holds, rules)}</td>;
}
