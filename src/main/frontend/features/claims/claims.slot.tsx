import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonClaims } from './PersonClaims';

export const contributions = [
  PersonColumnHeads.fill({ order: 20, Component: () => <th>Claims</th> }),
  PersonColumns.fill({ order: 20, Component: PersonClaims }),
];
