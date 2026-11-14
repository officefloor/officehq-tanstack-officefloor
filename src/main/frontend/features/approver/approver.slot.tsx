import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { PersonApprover } from './PersonApprover';

export const contributions = [
  PersonColumnHeads.fill({ order: 19.95, Component: () => <th>Approver</th> }),
  PersonColumns.fill({ order: 19.95, Component: PersonApprover }),
];
