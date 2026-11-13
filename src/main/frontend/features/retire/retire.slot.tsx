import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { BulkRetire } from './BulkRetire';
import { PersonRetire } from './PersonRetire';
import { ShowRetired } from './ShowRetired';

export const contributions = [
  HomeMain.fill({ order: 17, Component: ShowRetired }),
  HomeMain.fill({ order: 24.5, Component: BulkRetire }),
  PersonColumnHeads.fill({ order: 18, Component: () => <th>Retired</th> }),
  PersonColumns.fill({ order: 18, Component: PersonRetire }),
];
