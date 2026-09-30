import { useQuery } from '@tanstack/react-query';
import { dashboardKey, fetchDashboardSummary } from './queries';

// The dashboard's headline figures. Queries for itself under ['dashboard'] — never handed its data
// by a parent (CLAUDE.md rule 5). The outstanding total renders with 2 decimals like every other
// monetary figure in the app. Each value carries its stable data-testid (the test contract).
export function DashboardSummary() {
  const { data } = useQuery({ queryKey: dashboardKey, queryFn: fetchDashboardSummary });

  if (!data) {
    return null;
  }

  return (
    <dl data-testid="dashboard-summary">
      <div>
        <dt>Clients</dt>
        <dd data-testid="dashboard-clients-count">{String(data.clients)}</dd>
      </div>
      <div>
        <dt>Projects</dt>
        <dd data-testid="dashboard-projects-count">{String(data.projects)}</dd>
      </div>
      <div>
        <dt>Outstanding</dt>
        <dd data-testid="dashboard-outstanding-total">{Number(data.outstanding).toFixed(2)}</dd>
      </div>
    </dl>
  );
}
