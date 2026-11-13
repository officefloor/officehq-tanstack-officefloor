import type { Person } from '../people/people';
import { claimed, holdsById, useClaims } from './claims';

// What a person has claimed: the sum of their counting claims' amounts (not their open ones a hold covers).
export function PersonClaimed({ person }: { person: Person }) {
  const claims = (useClaims().data ?? []).filter((c) => c.personId === person.id);
  return <td data-testid="person-claimed">{claimed(claims, holdsById([person]))}</td>;
}
