import { SheetPanels } from '../../slots/defs/sheetPanels';
import { useSheetTotals } from './useSheetTotals';

/** The sheet's totals (see useSheetTotals). */
function ClaimedTotal({ sheet }: { user: string; sheet: string }) {
  const { claimedTotal, dueTotal, overdueCount, overdueTotal } = useSheetTotals(sheet);
  return (
    <>
      <p>
        Claimed in total: <strong data-testid="rollup-claimed-total">{claimedTotal}</strong>
      </p>
      <p>
        Due in total: <strong data-testid="rollup-due-total">{dueTotal}</strong>
      </p>
      <p>
        Overdue: <strong data-testid="rollup-overdue-count">{overdueCount}</strong>, due{' '}
        <strong data-testid="rollup-overdue-total">{overdueTotal}</strong>
      </p>
    </>
  );
}

export const contribution = SheetPanels.fill({ order: 20, Component: ClaimedTotal });
