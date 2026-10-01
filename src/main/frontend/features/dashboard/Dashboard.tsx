import { useQuery } from '@tanstack/react-query';
import { dashboardKey, getDashboard } from './api';
import { formatMoney } from '../../ui/money';

// The home dashboard: three figures summarising the whole workspace. Reads server data under
// ['dashboard'] (never copied into state); the figures are computed server-side so the page holds
// no arithmetic — it just renders. The outstanding total is money, shown with two decimal places.
export function Dashboard() {
  const { data } = useQuery({ queryKey: dashboardKey, queryFn: getDashboard });

  if (!data) {
    return null;
  }

  return (
    <dl data-testid="dashboard">
      <dt>Clients</dt>
      <dd data-testid="dashboard-clients-count">{data.clients}</dd>
      <dt>Projects</dt>
      <dd data-testid="dashboard-projects-count">{data.projects}</dd>
      <dt>Outstanding</dt>
      <dd data-testid="dashboard-outstanding-total">{formatMoney(data.outstanding)}</dd>
      <dt>Overdue</dt>
      <dd data-testid="dashboard-overdue-count">{data.overdue}</dd>
    </dl>
  );
}
