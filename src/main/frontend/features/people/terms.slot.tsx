import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import type { Person } from './people';
import { useColumnShown } from './columns';

// A person's own terms in days, blank when they have none.
function TermsHeader() {
  if (!useColumnShown('terms')) return null;
  return <th>Terms</th>;
}

function PersonTerms({ person }: { person: Person }) {
  const shown = useColumnShown('terms');
  if (!shown) return null;
  return <td data-testid="person-terms">{person.terms ?? ''}</td>;
}

export const contributions = [
  PeopleColumns.fill({ order: 5, Component: TermsHeader }),
  PersonCells.fill({ order: 5, Component: PersonTerms }),
];
