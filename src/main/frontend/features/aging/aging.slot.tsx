import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonAged0to30, PersonAged31to60, PersonAged61 } from './PersonAged';

export const contributions = [
  PersonColumnHeads.fill({ order: 17.1, Component: () => <th>0–30 days</th> }),
  PersonColumns.fill({ order: 17.1, Component: PersonAged0to30 }),
  PersonColumnHeads.fill({ order: 17.2, Component: () => <th>31–60 days</th> }),
  PersonColumns.fill({ order: 17.2, Component: PersonAged31to60 }),
  PersonColumnHeads.fill({ order: 17.3, Component: () => <th>61+ days</th> }),
  PersonColumns.fill({ order: 17.3, Component: PersonAged61 }),
];
