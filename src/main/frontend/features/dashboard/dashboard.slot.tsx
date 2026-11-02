import { SheetPanels } from '../../slots/defs/sheetPanels';
import { usePeople } from '../people/people';
import { counted, overdue, statusCount, useClaims, useLate } from '../claims/claims';
import { useUserRole } from '../roles/roles';

// A steward's dashboard over the sheet: how many claims are PENDING, how many are in REVIEW, and how many of the
// claims that count (see counted) are overdue. A user not acting as a steward sees the cells empty. Under an as-of
// date every read is the sheet as it stood then, so the counts and the role are those of then.
function Dashboard({ sheet, user }: { sheet: string; user: string }) {
  const { data: claims = [] } = useClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const { data: role } = useUserRole(sheet, user);
  const late = useLate(sheet);
  const steward = role?.role === 'steward';

  return (
    <section>
      <h2>Dashboard</h2>
      <p>
        Pending: <strong data-testid="dash-pending-count">{steward ? statusCount(claims, 'PENDING') : ''}</strong>{' '}
        Review: <strong data-testid="dash-review-count">{steward ? statusCount(claims, 'REVIEW') : ''}</strong>{' '}
        Overdue:{' '}
        <strong data-testid="dash-overdue-count">
          {steward ? counted(claims, people).filter((c) => overdue(c, late)).length : ''}
        </strong>
      </p>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 29, Component: Dashboard });
