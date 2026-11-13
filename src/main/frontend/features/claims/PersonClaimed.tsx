import type { Person } from '../people/people';
import { claimed, useClaims } from './claims';

// What a person has claimed: the sum of their claims' amounts.
export function PersonClaimed({ person }: { person: Person }) {
  const claims = (useClaims().data ?? []).filter((c) => c.personId === person.id);
  return <td data-testid="person-claimed">{claimed(claims)}</td>;
}
