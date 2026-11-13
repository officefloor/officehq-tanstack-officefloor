import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonHold } from './PersonHold';

export const contributions = [
  PersonColumnHeads.fill({ order: 19, Component: () => <th>Hold</th> }),
  PersonColumns.fill({ order: 19, Component: PersonHold }),
];
