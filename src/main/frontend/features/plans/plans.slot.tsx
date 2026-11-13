import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { GeneratePlans } from './GeneratePlans';
import { PersonPlans } from './PersonPlans';

export const contributions = [
  HomeMain.fill({ order: 16.5, Component: GeneratePlans }),
  PersonColumnHeads.fill({ order: 19.7, Component: () => <th>Plans</th> }),
  PersonColumns.fill({ order: 19.7, Component: PersonPlans }),
];
