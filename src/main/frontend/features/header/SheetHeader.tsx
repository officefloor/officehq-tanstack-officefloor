import { asString, useSearchParam } from '../../url/useSearchParam';
import { STATUSES, useSheetFigures } from '../people/useSheetFigures';

// The sheet's counters, always on show: its people (retired included), its claims per status (as
// the summary counts them) and its due-total and overdue-total (as the sheet's totals show them).
export function SheetHeader() {
  const [sheet] = useSearchParam('sheet', asString);
  const { people, overdue, overdueTotal, sheetDueTotal, statusCount } = useSheetFigures(sheet);
  return (
    <header data-testid="header">
      People: <span data-testid="header-people-count">{people.length}</span>
      {STATUSES.map((s) => (
        <span key={s}>
          {' · '}
          {s}: <span data-testid={`header-claims-${s}-count`}>{statusCount(s)}</span>
        </span>
      ))}
      {' · '}Due: <span data-testid="header-due-total">{sheetDueTotal}</span>
      {' · '}Overdue: <span data-testid="header-overdue-total">{overdueTotal(overdue)}</span>
    </header>
  );
}
