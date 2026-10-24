import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useSheetToday } from '../session/session';
import type { Person } from '../people/people';
import { holdsOf, sumOf, useClaims } from './claims';
import { useColumnShown } from '../people/columns';

// A person's claimed: the sum of their OPEN, PENDING and SETTLED claims' amounts (not corrections, nor unbound OPEN ones dated within a hold of theirs).
function ClaimedHeader() {
  if (!useColumnShown('claimed')) return null;
  return <th>Claimed</th>;
}

function PersonClaimed({ person }: { person: Person }) {
  const shown = useColumnShown('claimed');
  const claims = useClaims();
  const today = useSheetToday();
  if (!shown) return null;
  return <td data-testid="person-claimed">{sumOf((claims.data ?? []).filter((c) => c.personId === person.id), holdsOf([person]), today)}</td>;
}

export const contributions = [
  PeopleColumns.fill({ order: 10, Component: ClaimedHeader }),
  PersonCells.fill({ order: 10, Component: PersonClaimed }),
];
