import { SheetPanels } from '../../slots/defs/sheetPanels';
import { useClaims } from '../claims/useClaims';
import { useSheetTotals } from '../claims/useSheetTotals';
import { useRoleOf } from '../settings/useSettings';

/**
 * A steward's dashboard: how many of the sheet's claims are PENDING, how many are in REVIEW, and how
 * many of the claims that count are overdue (as the totals count them). Empty for a user who is not
 * a steward. Under an as-of date, the counts as they stood then, for the role the user had then.
 */
function StewardDashboard({ user, sheet }: { user: string; sheet: string }) {
  const { data: all = [] } = useClaims(sheet);
  const { overdueCount } = useSheetTotals(sheet);
  const steward = useRoleOf(sheet, user) === 'steward';
  const count = (n: number) => (steward ? String(n) : '');
  return (
    <section data-testid="steward-dashboard">
      <h2>Steward dashboard</h2>
      <span>
        Pending: <strong data-testid="dash-pending-count">{count(all.filter((c) => c.status === 'PENDING').length)}</strong>
      </span>{' '}
      <span>
        Review: <strong data-testid="dash-review-count">{count(all.filter((c) => c.status === 'REVIEW').length)}</strong>
      </span>{' '}
      <span>
        Overdue: <strong data-testid="dash-overdue-count">{count(overdueCount)}</strong>
      </span>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 2, Component: StewardDashboard });
