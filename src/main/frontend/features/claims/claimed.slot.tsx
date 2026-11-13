import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonClaimed } from './PersonClaimed';

export const contributions = [
  PersonColumnHeads.fill({ order: 10, Component: () => <th>Claimed</th> }),
  PersonColumns.fill({ order: 10, Component: PersonClaimed }),
];
