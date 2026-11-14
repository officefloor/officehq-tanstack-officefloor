import { useClaims } from '../claims/claims';
import { liveHolds, usePeople } from '../people/people';
import type { Person } from '../people/people';
import { useDueRules } from '../settings/settings';
import { personExcess, useRates } from './rates';
import { useColumnShown } from '../columns/columns';

// A person's excess: by how much the figures of their claims that count are above the sheet's cap; 0.00 otherwise.
export function PersonExcess({ person }: { person: Person }) {
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  const all = useClaims().data ?? [];
  const people = usePeople().data ?? [];
  const claims = all.filter((c) => c.personId === person.id);
  const shared = { personId: person.id, claims: all, people };
  if (!useColumnShown('excess')) return null;
  return <td data-testid="person-excess">{personExcess(claims, rates, person.grades, liveHolds(person, rules.today), rules, shared)}</td>;
}
