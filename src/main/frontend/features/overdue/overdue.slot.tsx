import { ClaimRowValues } from '../../slots/defs/claimRow';
import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { ClaimDueBy } from './ClaimDueBy';
import { ClaimOverdue } from './ClaimOverdue';
import { OverdueTotals } from './OverdueTotals';
import { PersonOverdueCount, PersonOverdueTotal } from './PersonOverdue';
import { ColumnHead } from '../columns/ColumnHead';

export const contributions = [
  PersonColumnHeads.fill({ order: 16, Component: () => <ColumnHead column="overdue-count">Overdue</ColumnHead> }),
  PersonColumns.fill({ order: 16, Component: PersonOverdueCount }),
  PersonColumnHeads.fill({ order: 17, Component: () => <ColumnHead column="overdue-total">Overdue total</ColumnHead> }),
  PersonColumns.fill({ order: 17, Component: PersonOverdueTotal }),
  ClaimRowValues.fill({ order: 12, Component: ClaimDueBy }),
  ClaimRowValues.fill({ order: 13, Component: ClaimOverdue }),
  HomeMain.fill({ order: 45, Component: OverdueTotals }),
];
