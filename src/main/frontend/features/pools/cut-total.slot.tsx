import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import type { Person } from '../people/people';
import { twoDecimals } from '../rates/rates';
import { personCutCents, usePools } from './pools';
import { useSheetTotals } from '../claims/claims';
import { Fig } from '../locale/Fig';

function CutTotalHeader() {
  return <th>Cut total</th>;
}

function PersonCutTotal({ sheet, person }: { sheet: string; user: string; person: Person }) {
  const { data: pools = [] } = usePools(sheet);
  return <td data-testid="person-cut-total"><Fig>{twoDecimals(personCutCents(pools, person.id))}</Fig></td>;
}

// The sheet's cut total: every person's cut total, trimmed (see useSheetTotals).
function SheetCutTotal({ sheet }: { sheet: string; user: string }) {
  const { cut } = useSheetTotals(sheet);
  return (
    <p>
      Cut total: <strong data-testid="rollup-cut-total"><Fig>{twoDecimals(cut)}</Fig></strong>
    </p>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 9.8, Component: CutTotalHeader }),
  PersonColumns.fill({ order: 9.8, Component: PersonCutTotal }),
  SheetPanels.fill({ order: 20.1, Component: SheetCutTotal }),
];
