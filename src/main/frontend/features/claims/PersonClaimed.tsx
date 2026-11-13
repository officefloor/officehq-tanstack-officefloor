import type { Person } from '../people/people';
import { useSheetToday } from '../session/sheetToday';
import { claimed, holdsById, useClaims } from './claims';

// What a person has claimed: the sum of their counting claims' amounts (not their open ones a hold covers).
export function PersonClaimed({ person }: { person: Person }) {
  const claims = (useClaims().data ?? []).filter((c) => c.personId === person.id);
  const today = useSheetToday();
  return <td data-testid="person-claimed">{claimed(claims, holdsById([person]), today)}</td>;
}
