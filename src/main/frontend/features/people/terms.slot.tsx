import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import type { Person } from './people';

// A person's own terms in days, blank when they have none.
function TermsHeader() {
  return <th>Terms</th>;
}

function PersonTerms({ person }: { person: Person }) {
  return <td data-testid="person-terms">{person.terms ?? ''}</td>;
}

export const contributions = [
  PeopleColumns.fill({ order: 5, Component: TermsHeader }),
  PersonCells.fill({ order: 5, Component: PersonTerms }),
];
