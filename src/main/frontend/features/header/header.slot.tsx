import { SheetPage } from '../../slots/defs/sheetPage';
import { usePeople } from '../people/people';
import { STATUSES, useSheetDueTotal, useSheetOverdue, useStatusCounts } from './totals';

/** The sheet's header counters, at the top of the sheet: its people (retired included), its claims per status
 *  (as the summary counts them), and its due-total and overdue-total (as the rollups show them). */
function Header({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId);
  const withStatus = useStatusCounts(sheetId);
  const dueTotal = useSheetDueTotal(sheetId);
  const overdue = useSheetOverdue(sheetId);

  return (
    <header data-testid="header">
      People <span data-testid="header-people-count">{(people.data ?? []).length}</span>
      {STATUSES.map((s) => (
        <span key={s}>
          {' · '}
          {s} <span data-testid={`header-claims-${s}-count`}>{withStatus(s)}</span>
        </span>
      ))}
      {' · '}Due total <span data-testid="header-due-total">{dueTotal}</span>
      {' · '}Overdue total <span data-testid="header-overdue-total">{overdue.total}</span>
    </header>
  );
}

export const contribution = SheetPage.fill({ order: -10, Component: Header });
