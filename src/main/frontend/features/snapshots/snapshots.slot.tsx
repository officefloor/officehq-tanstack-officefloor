import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonSnapshots } from './PersonSnapshots';

export const contributions = [
  PersonColumnHeads.fill({ order: 15.1, Component: () => <th>Snapshots</th> }),
  PersonColumns.fill({ order: 15.1, Component: PersonSnapshots }),
];
