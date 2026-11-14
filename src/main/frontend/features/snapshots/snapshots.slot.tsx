import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonSnapshots } from './PersonSnapshots';
import { SnapshotCompare } from './SnapshotCompare';

export const contributions = [
  PersonColumnHeads.fill({ order: 15.1, Component: () => <th>Snapshots</th> }),
  PersonColumns.fill({ order: 15.1, Component: PersonSnapshots }),
  HomeMain.fill({ order: 31.1, Component: SnapshotCompare }),
];
