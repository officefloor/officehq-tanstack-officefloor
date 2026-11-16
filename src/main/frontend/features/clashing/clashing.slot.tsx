import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import type { Person } from '../people/people';

function ClashingHead() {
  return <th>Clashing</th>;
}

/** Whether the person's name or an alias is used on another sheet of the sheet's tree (decided by the server). */
function PersonClashing({ person }: { sheetId: string; user: string; person: Person }) {
  return <td data-testid="person-clashing">{person.clashing ? 'yes' : 'no'}</td>;
}

export const contributions = [
  PeopleHead.fill({ order: 6, Component: ClashingHead }),
  PersonRow.fill({ order: 6, Component: PersonClashing }),
];
