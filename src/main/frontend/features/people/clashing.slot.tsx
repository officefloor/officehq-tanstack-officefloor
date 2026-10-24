import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import type { Person } from './people';

// Whether the person clashes: their name or an alias is in use on another sheet of the sheet's tree. yes / no.
function ClashingHeader() {
  return <th>Clashing</th>;
}

function PersonClashing({ person }: { person: Person }) {
  return <td data-testid="person-clashing">{person.clashing ? 'yes' : 'no'}</td>;
}

export const contributions = [
  PeopleColumns.fill({ order: 6.7, Component: ClashingHeader }),
  PersonCells.fill({ order: 6.7, Component: PersonClashing }),
];
