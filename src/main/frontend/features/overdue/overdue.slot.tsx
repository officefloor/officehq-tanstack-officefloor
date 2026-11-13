import { ClaimRowValues } from '../../slots/defs/claimRow';
import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { ClaimDueBy } from './ClaimDueBy';
import { ClaimOverdue } from './ClaimOverdue';
import { OverdueTotals } from './OverdueTotals';
import { PersonOverdueCount, PersonOverdueTotal } from './PersonOverdue';

export const contributions = [
  PersonColumnHeads.fill({ order: 16, Component: () => <th>Overdue</th> }),
  PersonColumns.fill({ order: 16, Component: PersonOverdueCount }),
  PersonColumnHeads.fill({ order: 17, Component: () => <th>Overdue total</th> }),
  PersonColumns.fill({ order: 17, Component: PersonOverdueTotal }),
  ClaimRowValues.fill({ order: 12, Component: ClaimDueBy }),
  ClaimRowValues.fill({ order: 13, Component: ClaimOverdue }),
  HomeMain.fill({ order: 45, Component: OverdueTotals }),
];
