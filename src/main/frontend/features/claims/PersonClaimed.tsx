import type { Person } from '../people/people';
import { useSheetToday } from '../session/sheetToday';
import { claimed, holdsById, useClaims } from './claims';
import { useColumnShown } from '../columns/columns';

// What a person has claimed: the sum of their counting claims' amounts (not corrections, nor their open ones a hold covers).
export function PersonClaimed({ person }: { person: Person }) {
  const claims = (useClaims().data ?? []).filter((c) => c.personId === person.id);
  const today = useSheetToday();
  if (!useColumnShown('claimed')) return null;
  return <td data-testid="person-claimed">{claimed(claims, holdsById([person], today), today)}</td>;
}
