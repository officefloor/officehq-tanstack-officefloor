import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { CutTotal, PersonCutTotal } from './CutTotals';

export const contributions = [
  PersonColumnHeads.fill({ order: 15.7, Component: () => <th>Cut</th> }),
  PersonColumns.fill({ order: 15.7, Component: PersonCutTotal }),
  HomeMain.fill({ order: 43, Component: CutTotal }),
];
