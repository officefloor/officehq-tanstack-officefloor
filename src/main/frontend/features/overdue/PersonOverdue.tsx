import { useClaims } from '../claims/claims';
import { liveHolds } from '../people/people';
import type { Person } from '../people/people';
import { useRates } from '../rates/rates';
import { useDueRules } from '../settings/settings';
import { personOverdue } from './overdue';
import { useColumnShown } from '../columns/columns';

function usePersonOverdue(person: Person) {
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  const claims = (useClaims().data ?? []).filter((c) => c.personId === person.id);
  return personOverdue(claims, rates, person.grades, liveHolds(person, rules.today), rules);
}

// How many of a person's counting claims are overdue.
export function PersonOverdueCount({ person }: { person: Person }) {
  const { count } = usePersonOverdue(person);
  return useColumnShown('overdue-count') ? <td data-testid="person-overdue-count">{count}</td> : null;
}

// The dues (late charges included) of a person's overdue counting claims, added up.
export function PersonOverdueTotal({ person }: { person: Person }) {
  const { total } = usePersonOverdue(person);
  return useColumnShown('overdue-total') ? <td data-testid="person-overdue-total">{total}</td> : null;
}
