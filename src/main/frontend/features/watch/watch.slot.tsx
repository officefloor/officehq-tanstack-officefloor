import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonWatch } from './PersonWatch';

export const contributions = [
  PersonColumnHeads.fill({ order: 19.8, Component: () => <th>Watched</th> }),
  PersonColumns.fill({ order: 19.8, Component: PersonWatch }),
];
