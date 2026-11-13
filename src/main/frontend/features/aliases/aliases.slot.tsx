import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonAliases } from './PersonAliases';

export const contributions = [
  PersonColumnHeads.fill({ order: 1, Component: () => <th>Aliases</th> }),
  PersonColumns.fill({ order: 1, Component: PersonAliases }),
];
