import { useClaims } from '../claims/claims';
import { liveHolds } from '../people/people';
import type { Person } from '../people/people';
import { useDueRules } from '../settings/settings';
import { isActive } from './active';

// Whether a person is active: yes or no.
export function PersonActive({ person }: { person: Person }) {
  const rules = useDueRules();
  const claims = (useClaims().data ?? []).filter((c) => c.personId === person.id);
  return <td data-testid="person-active">{isActive(claims, liveHolds(person, rules.today), rules.today) ? 'yes' : 'no'}</td>;
}
