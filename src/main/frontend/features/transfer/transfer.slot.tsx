import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import type { Person } from '../people/people';
import { PersonTransfer } from './PersonTransfer';

// The sheet a transferred person came from (blank for anyone not transferred here).
function PersonOrigin({ person }: { person: Person }) {
  return <td data-testid="person-origin">{person.origin ?? ''}</td>;
}

export const contributions = [
  PersonColumnHeads.fill({ order: 18.55, Component: () => <th>From</th> }),
  PersonColumns.fill({ order: 18.55, Component: PersonOrigin }),
  PersonColumnHeads.fill({ order: 18.6, Component: () => <th>Transfer</th> }),
  PersonColumns.fill({ order: 18.6, Component: PersonTransfer }),
];
