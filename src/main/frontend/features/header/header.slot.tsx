import { useQuery } from '@tanstack/react-query';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsQuery } from '../claims/claimsQuery';
import { peopleQuery } from '../people/peopleQuery';
import { useSheetDueTotal } from '../rates/due.slot';
import { useSheetOverdue } from '../rates/overdue.slot';
import { STATUSES, statusCount } from '../summary/summary.slot';

/**
 * Counters shown at the top of the sheet at all times. Each repeats a figure shown elsewhere and is
 * worked out the same way: people on the sheet (retired included), claims per status as in the
 * summary, and the sheet's due- and overdue-totals.
 */
function Header({ sheetId }: { sheetId: string }) {
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  const dueTotal = useSheetDueTotal(sheetId);
  const { total: overdueTotal } = useSheetOverdue(sheetId);

  return (
    <header data-testid="header">
      People <strong data-testid="header-people-count">{people.length}</strong>
      {STATUSES.map((s) => (
        <span key={s}>
          {' '}· {s} <strong data-testid={`header-claims-${s}-count`}>{statusCount(claims, s)}</strong>
        </span>
      ))}
      {' '}· Due <strong data-testid="header-due-total">{dueTotal}</strong>
      {' '}· Overdue <strong data-testid="header-overdue-total">{overdueTotal}</strong>
    </header>
  );
}

export const contribution = SheetPanel.fill({ order: -10, Component: Header });
