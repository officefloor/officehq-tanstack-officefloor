import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonOpenTotal, PersonSettledTotal } from './PersonSplit';
import { SplitTotals } from './SplitTotals';

export const contributions = [
  PersonColumnHeads.fill({ order: 15.2, Component: () => <th>Settled</th> }),
  PersonColumns.fill({ order: 15.2, Component: PersonSettledTotal }),
  PersonColumnHeads.fill({ order: 15.3, Component: () => <th>Open</th> }),
  PersonColumns.fill({ order: 15.3, Component: PersonOpenTotal }),
  HomeMain.fill({ order: 42, Component: SplitTotals }),
];
