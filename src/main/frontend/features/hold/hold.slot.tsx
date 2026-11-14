import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { BulkHold } from './BulkHold';
import { PersonHold } from './PersonHold';

export const contributions = [
  HomeMain.fill({ order: 24.6, Component: BulkHold }),
  PersonColumnHeads.fill({ order: 19, Component: () => <th>Hold</th> }),
  PersonColumns.fill({ order: 19, Component: PersonHold }),
];
