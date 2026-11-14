import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonFlag } from './PersonFlag';

export const contributions = [
  PersonColumnHeads.fill({ order: 19.9, Component: () => <th>Flag</th> }),
  PersonColumns.fill({ order: 19.9, Component: PersonFlag }),
];
