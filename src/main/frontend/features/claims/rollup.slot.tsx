import { SheetPanels } from '../../slots/defs/sheetPanels';
import { twoDecimals } from '../rates/rates';
import { useMonthDueTotals, useSheetTotals, useTagDueTotals } from './claims';

// The sheet's claimed-total, due-total, overdue count and total, and settled-total and open-total (see useSheetTotals),
// the due-total of each tag ever used on the sheet (see useTagDueTotals), and the due-total of each month any claim
// is dated in (see useMonthDueTotals).
function ClaimedTotal({ sheet }: { sheet: string; user: string }) {
  const { claimed, due, overdue, settled, open } = useSheetTotals(sheet);
  const tags = useTagDueTotals(sheet);
  const months = useMonthDueTotals(sheet);
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
      {tags.length > 0 && (
        <ul>
          {tags.map(({ tag, cents }) => (
            <li key={tag}>
              Tag {tag} due total: <strong data-testid={`tag-${tag}-due-total`}>{twoDecimals(cents)}</strong>
            </li>
          ))}
        </ul>
      )}
      {months.length > 0 && (
        <ul>
          {months.map(({ month, cents }) => (
            <li key={month}>
              Month {month} due total: <strong data-testid={`month-${month}-due-total`}>{twoDecimals(cents)}</strong>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}

export const contribution = SheetPanels.fill({ order: 20, Component: ClaimedTotal });
