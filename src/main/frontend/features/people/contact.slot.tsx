import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import type { Person } from './people';
import { useColumnShown } from './columns';

// A person's contact, blank when they have none.
function ContactHeader() {
  if (!useColumnShown('contact')) return null;
  return <th>Contact</th>;
}

function PersonContact({ person }: { person: Person }) {
  const shown = useColumnShown('contact');
  if (!shown) return null;
  return <td data-testid="person-contact">{person.contact ?? ''}</td>;
}

export const contributions = [
  PeopleColumns.fill({ order: 6, Component: ContactHeader }),
  PersonCells.fill({ order: 6, Component: PersonContact }),
];
