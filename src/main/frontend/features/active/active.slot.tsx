import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonActive } from './PersonActive';

export const contributions = [
  PersonColumnHeads.fill({ order: 21, Component: () => <th>Active</th> }),
  PersonColumns.fill({ order: 21, Component: PersonActive }),
];
