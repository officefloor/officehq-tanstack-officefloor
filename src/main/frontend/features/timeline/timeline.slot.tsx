import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonTimeline } from './PersonTimeline';

export const contributions = [
  PersonColumnHeads.fill({ order: 19.97, Component: () => <th>Timeline</th> }),
  PersonColumns.fill({ order: 19.97, Component: PersonTimeline }),
];
