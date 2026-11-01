import { SheetPanels } from '../../slots/defs/sheetPanels';
import { twoDecimals } from '../rates/rates';
import { useSheetTotals } from './claims';

// The sheet's claimed-total, due-total, overdue count and total, and settled-total and open-total (see useSheetTotals).
function ClaimedTotal({ sheet }: { sheet: string; user: string }) {
  const { claimed, due, overdue, settled, open } = useSheetTotals(sheet);
  return (
    <>
      <p>
        Claimed total: <strong data-testid="rollup-claimed-total">{claimed}</strong>
      </p>
      <p>
        Due total: <strong data-testid="rollup-due-total">{twoDecimals(due)}</strong>
      </p>
      <p>
        Overdue: <strong data-testid="rollup-overdue-count">{overdue.count}</strong>, total{' '}
        <strong data-testid="rollup-overdue-total">{twoDecimals(overdue.cents)}</strong>
      </p>
      <p>
        Settled total: <strong data-testid="rollup-settled-total">{twoDecimals(settled)}</strong>, open total{' '}
        <strong data-testid="rollup-open-total">{twoDecimals(open)}</strong>
      </p>
    </>
  );
}

export const contribution = SheetPanels.fill({ order: 20, Component: ClaimedTotal });
