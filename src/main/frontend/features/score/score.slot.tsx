import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonBand, PersonScore } from './PersonScore';

export const contributions = [
  PersonColumnHeads.fill({ order: 17.5, Component: () => <th>Score</th> }),
  PersonColumns.fill({ order: 17.5, Component: PersonScore }),
  PersonColumnHeads.fill({ order: 17.6, Component: () => <th>Band</th> }),
  PersonColumns.fill({ order: 17.6, Component: PersonBand }),
];
