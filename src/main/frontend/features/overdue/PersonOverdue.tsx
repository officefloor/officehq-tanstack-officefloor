import { useClaims } from '../claims/claims';
import type { Person } from '../people/people';
import { useRates } from '../rates/rates';
import { useDueRules } from '../settings/settings';
import { personOverdue } from './overdue';

function usePersonOverdue(person: Person) {
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  const claims = (useClaims().data ?? []).filter((c) => c.personId === person.id);
  return personOverdue(claims, rates, person.grades, person.held, rules);
}

// How many of a person's counting claims are overdue.
export function PersonOverdueCount({ person }: { person: Person }) {
  return <td data-testid="person-overdue-count">{usePersonOverdue(person).count}</td>;
}

// The dues (late charges included) of a person's overdue counting claims, added up.
export function PersonOverdueTotal({ person }: { person: Person }) {
  return <td data-testid="person-overdue-total">{usePersonOverdue(person).total}</td>;
}
