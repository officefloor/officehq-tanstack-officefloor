import { ClaimRowValues } from '../../slots/defs/claimRow';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { ClaimDue } from './ClaimDue';
import { PersonDueTotal } from './PersonDueTotal';
import { PersonExcess } from './PersonExcess';
import { ColumnHead } from '../columns/ColumnHead';

export const contributions = [
  PersonColumnHeads.fill({ order: 15, Component: () => <ColumnHead column="due-total">Due</ColumnHead> }),
  PersonColumns.fill({ order: 15, Component: PersonDueTotal }),
  PersonColumnHeads.fill({ order: 15.5, Component: () => <ColumnHead column="excess">Excess</ColumnHead> }),
  PersonColumns.fill({ order: 15.5, Component: PersonExcess }),
  ClaimRowValues.fill({ order: 10, Component: ClaimDue }),
];
