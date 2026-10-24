import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import type { Person } from './people';

// The sheet a transferred person came from; empty for anyone not transferred here.
function OriginHeader() {
  return <th>From sheet</th>;
}

function PersonOrigin({ person }: { person: Person }) {
  return <td data-testid="person-origin">{person.origin ?? ''}</td>;
}

export const contributions = [
  PeopleColumns.fill({ order: 6.8, Component: OriginHeader }),
  PersonCells.fill({ order: 6.8, Component: PersonOrigin }),
];
