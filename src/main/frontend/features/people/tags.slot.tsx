import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import type { Person } from './people';

// A person's tags, sorted and comma separated; blank when they have none.
function TagsHeader() {
  return <th>Tags</th>;
}

function PersonTags({ person }: { person: Person }) {
  return <td data-testid="person-tags">{(person.tags ?? []).join(', ')}</td>;
}

export const contributions = [
  PeopleColumns.fill({ order: 6.9, Component: TagsHeader }),
  PersonCells.fill({ order: 6.9, Component: PersonTags }),
];
