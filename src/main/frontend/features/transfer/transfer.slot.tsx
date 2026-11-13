import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonTransfer } from './PersonTransfer';

export const contributions = [
  PersonColumnHeads.fill({ order: 18.6, Component: () => <th>Transfer</th> }),
  PersonColumns.fill({ order: 18.6, Component: PersonTransfer }),
];
