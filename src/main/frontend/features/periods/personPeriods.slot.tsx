import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonPeriodDue, PersonPeriodHeads } from './PersonPeriodDue';

export const contributions = [
  PersonColumnHeads.fill({ order: 15.7, Component: PersonPeriodHeads }),
  PersonColumns.fill({ order: 15.7, Component: PersonPeriodDue }),
];
