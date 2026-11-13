import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonMerge } from './PersonMerge';

export const contributions = [
  PersonColumnHeads.fill({ order: 18.5, Component: () => <th>Merge</th> }),
  PersonColumns.fill({ order: 18.5, Component: PersonMerge }),
];
