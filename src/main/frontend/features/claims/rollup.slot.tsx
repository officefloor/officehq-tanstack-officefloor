import { SheetPanels } from '../../slots/defs/sheetPanels';
import { twoDecimals } from '../rates/rates';
import { useMonthDueTotals, useSheetTotals, useTagDueTotals } from './claims';
import { Fig } from '../locale/Fig';

// The sheet's claimed-total, due-total, overdue count and total, settled-total and open-total (trimmed), and age buckets (see useSheetTotals),
// the due-total of each tag ever used on the sheet or carried by a person on it (see useTagDueTotals), and the due-total of each month any claim
// is dated in (see useMonthDueTotals).
function ClaimedTotal({ sheet }: { sheet: string; user: string }) {
  const { claimed, due, overdue, settled, open, aged } = useSheetTotals(sheet);
  const tags = useTagDueTotals(sheet);
  const months = useMonthDueTotals(sheet);
  return (
    <>
      <p>
        Claimed total: <strong data-testid="rollup-claimed-total"><Fig>{claimed}</Fig></strong>
      </p>
      <p>
        Due total: <strong data-testid="rollup-due-total"><Fig>{twoDecimals(due)}</Fig></strong>
      </p>
      <p>
        Overdue: <strong data-testid="rollup-overdue-count">{overdue.count}</strong>, total{' '}
        <strong data-testid="rollup-overdue-total"><Fig>{twoDecimals(overdue.cents)}</Fig></strong>
      </p>
      <p>
        Settled total: <strong data-testid="rollup-settled-total"><Fig>{twoDecimals(settled)}</Fig></strong>, open total{' '}
        <strong data-testid="rollup-open-total"><Fig>{twoDecimals(open)}</Fig></strong>
      </p>
      <p>
        Due by age: 0 to 30 days <strong data-testid="rollup-aged-0-30"><Fig>{twoDecimals(aged.upTo30)}</Fig></strong>, 31 to 60{' '}
        <strong data-testid="rollup-aged-31-60"><Fig>{twoDecimals(aged.upTo60)}</Fig></strong>, 61 or more{' '}
        <strong data-testid="rollup-aged-61"><Fig>{twoDecimals(aged.over60)}</Fig></strong>
      </p>
      {tags.length > 0 && (
        <ul>
          {tags.map(({ tag, cents }) => (
            <li key={tag}>
              Tag {tag} due total: <strong data-testid={`tag-${tag}-due-total`}><Fig>{twoDecimals(cents)}</Fig></strong>
            </li>
          ))}
        </ul>
      )}
      {months.length > 0 && (
        <ul>
          {months.map(({ month, cents }) => (
            <li key={month}>
              Month {month} due total: <strong data-testid={`month-${month}-due-total`}><Fig>{twoDecimals(cents)}</Fig></strong>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}

export const contribution = SheetPanels.fill({ order: 20, Component: ClaimedTotal });
