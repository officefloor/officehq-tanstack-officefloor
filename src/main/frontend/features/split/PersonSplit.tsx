import { useClaims } from '../claims/claims';
import { liveHolds } from '../people/people';
import type { Person } from '../people/people';
import { personSplit, useRates } from '../rates/rates';
import { useDueRules } from '../settings/settings';

function usePersonSplit(person: Person) {
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  const claims = (useClaims().data ?? []).filter((c) => c.personId === person.id);
  return personSplit(claims, rates, person.grades, liveHolds(person, rules.today), rules);
}

// The dues of a person's settled counting claims, added up.
export function PersonSettledTotal({ person }: { person: Person }) {
  return <td data-testid="person-settled-total">{usePersonSplit(person).settled}</td>;
}

// The dues of a person's open (and pending) counting claims, added up.
export function PersonOpenTotal({ person }: { person: Person }) {
  return <td data-testid="person-open-total">{usePersonSplit(person).open}</td>;
}
