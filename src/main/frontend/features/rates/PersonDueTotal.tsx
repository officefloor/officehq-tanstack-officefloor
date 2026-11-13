import { claimed, useClaims } from '../claims/claims';
import type { Person } from '../people/people';
import { due, rateFor, useRates } from './rates';

// A person's due-total: what they have claimed at the rate for their grade.
export function PersonDueTotal({ person }: { person: Person }) {
  const rates = useRates().data ?? [];
  const claims = (useClaims().data ?? []).filter((c) => c.personId === person.id);
  return <td data-testid="person-due-total">{due(claimed(claims), rateFor(rates, person.grade))}</td>;
}
