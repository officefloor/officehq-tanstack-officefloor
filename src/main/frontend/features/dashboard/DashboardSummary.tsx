import { useQuery } from '@tanstack/react-query';
import { dashboardKey, fetchDashboardSummary } from './queries';
import { formatMoney } from '../../ui/money';

// The dashboard's headline figures. Queries for itself under ['dashboard'] — never handed its data
// by a parent (CLAUDE.md rule 5). Clients are paid in different currencies, so the outstanding figure
// is kept SEPARATE per currency and never added together: one line per currency, each rendered with
// that currency's symbol (data-testid="dashboard-outstanding-<CODE>"). Each value carries its stable
// data-testid (the test contract).
export function DashboardSummary() {
  const { data } = useQuery({ queryKey: dashboardKey, queryFn: fetchDashboardSummary });

  if (!data) {
    return null;
  }

  const currencies = Object.keys(data.outstandingByCurrency).sort();

  return (
    <dl data-testid="dashboard-summary">
      <div>
        <dt>Clients</dt>
        <dd data-testid="dashboard-clients-count">{String(data.clients)}</dd>
      </div>
      <div>
        <dt>Jobs</dt>
        <dd data-testid="dashboard-projects-count">{String(data.projects)}</dd>
      </div>
      {currencies.map((currency) => (
        <div key={currency}>
          <dt>Outstanding ({currency})</dt>
          <dd data-testid={`dashboard-outstanding-${currency}`}>
            {formatMoney(data.outstandingByCurrency[currency], currency)}
          </dd>
        </div>
      ))}
      <div>
        <dt>Overdue invoices</dt>
        <dd data-testid="dashboard-overdue-count">{String(data.overdue)}</dd>
      </div>
    </dl>
  );
}
