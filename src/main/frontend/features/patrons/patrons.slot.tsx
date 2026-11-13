import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonBacking, PersonPatrons } from './PersonPatrons';

export const contributions = [
  PersonColumnHeads.fill({ order: 19.5, Component: () => <th>Patrons</th> }),
  PersonColumns.fill({ order: 19.5, Component: PersonPatrons }),
  PersonColumnHeads.fill({ order: 19.6, Component: () => <th>Backing</th> }),
  PersonColumns.fill({ order: 19.6, Component: PersonBacking }),
];
