import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import type { Person } from './people';

// A person's contact, blank when they have none.
function ContactHeader() {
  return <th>Contact</th>;
}

function PersonContact({ person }: { person: Person }) {
  return <td data-testid="person-contact">{person.contact ?? ''}</td>;
}

export const contributions = [
  PeopleColumns.fill({ order: 6, Component: ContactHeader }),
  PersonCells.fill({ order: 6, Component: PersonContact }),
];
