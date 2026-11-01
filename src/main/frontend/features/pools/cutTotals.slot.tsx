import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import type { Person } from '../people/usePeople';
import { showDue } from '../rates/useRates';
import { useSheetTotals } from '../claims/useSheetTotals';
import { cutTotalCents, usePools } from './usePools';

/** A person's cut total: their cuts across every pool they are a member of, added up (a pool that
 *  shows no cuts adds nothing). */
function PersonCutTotal({ sheet, person }: { user: string; sheet: string; person: Person }) {
  const { data: pools = [] } = usePools(sheet);
  return <td data-testid="person-cut-total">{showDue(cutTotalCents(pools, person.id))}</td>;
}

/** The sheet's cut total: everyone's cut total added up, trimmed (see useSheetTotals). */
function SheetCutTotal({ sheet }: { user: string; sheet: string }) {
  const { cutTotal } = useSheetTotals(sheet);
  return (
    <p>
      Cuts in total: <strong data-testid="rollup-cut-total">{cutTotal}</strong>
    </p>
  );
}

export const contributions = [
  PersonColumns.fill({ order: 13, Component: () => <th>Cut total</th> }),
  PersonCells.fill({ order: 13, Component: PersonCutTotal }),
  SheetPanels.fill({ order: 20.5, Component: SheetCutTotal }),
];
