import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonSelect } from './PersonSelect';

export const contributions = [
  PersonColumnHeads.fill({ order: -10, Component: () => <th>Select</th> }),
  PersonColumns.fill({ order: -10, Component: PersonSelect }),
];
