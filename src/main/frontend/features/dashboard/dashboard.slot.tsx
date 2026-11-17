import { SheetPage } from '../../slots/defs/sheetPage';
import { useSheetOverdue, useStatusCounts } from '../header/totals';
import { useActingRole } from '../roles/roles';

/** The steward's dashboard: how many of the sheet's claims are PENDING, how many REVIEW, and how many that count are
 *  overdue (as the rollups count them). Empty for a user who does not act as a steward; as of a date, the counts as
 *  they stood then, for the role the user had then. */
function Dashboard({ sheetId, user }: { sheetId: string; user: string }) {
  const role = useActingRole(sheetId, user);
  const withStatus = useStatusCounts(sheetId);
  const overdue = useSheetOverdue(sheetId);
  const steward = role.data?.role === 'steward';
  return (
    <section data-testid="dashboard">
      Pending <span data-testid="dash-pending-count">{steward ? withStatus('PENDING') : ''}</span>
      {' · '}Review <span data-testid="dash-review-count">{steward ? withStatus('REVIEW') : ''}</span>
      {' · '}Overdue <span data-testid="dash-overdue-count">{steward ? overdue.count : ''}</span>
    </section>
  );
}

export const contribution = SheetPage.fill({ order: -5, Component: Dashboard });
