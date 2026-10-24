import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import type { Person } from '../people/people';
import { sumOf, useClaims } from './claims';

// A person's claimed: the sum of their claims' amounts.
function ClaimedHeader() {
  return <th>Claimed</th>;
}

function PersonClaimed({ person }: { person: Person }) {
  const claims = useClaims();
  return <td data-testid="person-claimed">{sumOf((claims.data ?? []).filter((c) => c.personId === person.id))}</td>;
}

export const contributions = [
  PeopleColumns.fill({ order: 10, Component: ClaimedHeader }),
  PersonCells.fill({ order: 10, Component: PersonClaimed }),
];
