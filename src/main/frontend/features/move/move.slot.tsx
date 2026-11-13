import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonMove } from './PersonMove';

export const contributions = [
  PersonColumnHeads.fill({ order: 21.5, Component: () => <th>Move</th> }),
  PersonColumns.fill({ order: 21.5, Component: PersonMove }),
];
