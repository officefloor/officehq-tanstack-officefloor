import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonClaimed } from './PersonClaimed';
import { ColumnHead } from '../columns/ColumnHead';

export const contributions = [
  PersonColumnHeads.fill({ order: 10, Component: () => <ColumnHead column="claimed">Claimed</ColumnHead> }),
  PersonColumns.fill({ order: 10, Component: PersonClaimed }),
];
