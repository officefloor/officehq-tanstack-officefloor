import { useQuery } from '@tanstack/react-query';
import { dashboardKey, getDashboard } from './api';
import { formatMoney } from '../../ui/money';

// The home dashboard: three figures summarising the whole workspace. Reads server data under
// ['dashboard'] (never copied into state); the figures are computed server-side so the page holds
// no arithmetic — it just renders. What is outstanding is kept separate per currency (money is never
// added across currencies), each figure shown in its own currency with two decimal places.
export function Dashboard() {
  const { data } = useQuery({ queryKey: dashboardKey, queryFn: getDashboard });

  if (!data) {
    return null;
  }

  return (
    <dl data-testid="dashboard">
      <dt>Clients</dt>
      <dd data-testid="dashboard-clients-count">{data.clients}</dd>
      <dt>Jobs</dt>
      <dd data-testid="dashboard-projects-count">{data.projects}</dd>
      <dt>Outstanding</dt>
      {data.outstandingByCurrency.map((row) => (
        <dd key={row.currency} data-testid={`dashboard-outstanding-${row.currency}`}>
          {formatMoney(row.amount, row.currency)}
        </dd>
      ))}
      <dt>Overdue</dt>
      <dd data-testid="dashboard-overdue-count">{data.overdue}</dd>
    </dl>
  );
}
