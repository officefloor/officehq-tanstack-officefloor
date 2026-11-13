import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonRetire } from './PersonRetire';
import { ShowRetired } from './ShowRetired';

export const contributions = [
  HomeMain.fill({ order: 17, Component: ShowRetired }),
  PersonColumnHeads.fill({ order: 18, Component: () => <th>Retired</th> }),
  PersonColumns.fill({ order: 18, Component: PersonRetire }),
];
