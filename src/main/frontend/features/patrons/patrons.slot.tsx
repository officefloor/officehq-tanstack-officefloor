import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonBacking, PersonPatrons } from './PersonPatrons';
import { ColumnHead } from '../columns/ColumnHead';

export const contributions = [
  PersonColumnHeads.fill({ order: 19.5, Component: () => <th>Patrons</th> }),
  PersonColumns.fill({ order: 19.5, Component: PersonPatrons }),
  PersonColumnHeads.fill({ order: 19.6, Component: () => <ColumnHead column="backing">Backing</ColumnHead> }),
  PersonColumns.fill({ order: 19.6, Component: PersonBacking }),
];
