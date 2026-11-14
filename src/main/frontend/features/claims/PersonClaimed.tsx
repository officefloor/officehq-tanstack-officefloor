import type { Person } from '../people/people';
import { useSheetToday } from '../session/sheetToday';
import { claimed, useClaims } from './claims';
import { useColumnShown } from '../columns/columns';

// What a person has claimed: the sum of their counting claims' amounts (not corrections; held ones count, a retired
// person's none once their retirement date is reached).
export function PersonClaimed({ person }: { person: Person }) {
  const claims = (useClaims().data ?? []).filter((c) => c.personId === person.id);
  const today = useSheetToday();
  if (!useColumnShown('claimed')) return null;
  return <td data-testid="person-claimed">{claimed(claims, today, [person])}</td>;
}
