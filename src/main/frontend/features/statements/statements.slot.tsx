import { ClaimRowValues } from '../../slots/defs/claimRow';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { ClaimStated } from './ClaimStated';
import { PersonStatement } from './PersonStatement';

export const contributions = [
  PersonColumnHeads.fill({ order: 17, Component: () => <th>Statements</th> }),
  PersonColumns.fill({ order: 17, Component: PersonStatement }),
  ClaimRowValues.fill({ order: 12, Component: ClaimStated }),
];
