import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsKey } from '../claims/claimsQuery';

/** The counts, each null for a user who is not a steward. */
type Dashboard = { pending: number | null; review: number | null; overdue: number | null };

/** Under the claims' key, so whatever re-reads the claims re-reads the dashboard too. */
const dashboardQuery = (sheetId: string, user: string) => ({
  queryKey: [...claimsKey(sheetId), 'dashboard', user] as const,
  queryFn: () => getJson<Dashboard>(`/api/sheets/${encodeURIComponent(sheetId)}/dashboard`, user),
});

/**
 * The steward's dashboard: how many of the sheet's claims are PENDING, how many REVIEW, and how many that count are
 * overdue. A user who is not a steward (a delegation included) sees the cells empty; as of a date, the counts and the
 * role are as they stood then.
 */
function StewardDashboard({ user, sheetId }: { user: string; sheetId: string }) {
  const dash = useQuery(dashboardQuery(sheetId, user)).data;
  const shown = (n: number | null | undefined) => (n == null ? '' : String(n));

  return (
    <table data-testid="dashboard">
      <thead>
        <tr>
          <th>Pending</th>
          <th>Review</th>
          <th>Overdue</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td data-testid="dash-pending-count">{shown(dash?.pending)}</td>
          <td data-testid="dash-review-count">{shown(dash?.review)}</td>
          <td data-testid="dash-overdue-count">{shown(dash?.overdue)}</td>
        </tr>
      </tbody>
    </table>
  );
}

export const contribution = SheetPanel.fill({ order: 0.6, Component: StewardDashboard });
