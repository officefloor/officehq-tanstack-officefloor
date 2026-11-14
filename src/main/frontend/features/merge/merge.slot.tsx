import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonMerge } from './PersonMerge';
import { PersonMergeUndo } from './PersonMergeUndo';

export const contributions = [
  PersonColumnHeads.fill({ order: 18.5, Component: () => <th>Merge</th> }),
  PersonColumns.fill({ order: 18.5, Component: PersonMerge }),
  PersonColumnHeads.fill({ order: 18.52, Component: () => <th>Undo merge</th> }),
  PersonColumns.fill({ order: 18.52, Component: PersonMergeUndo }),
];
