import { Fragment } from 'react';
import { formatMoney } from '../../ui/money';
import { useDashboard } from './dashboard';

// The dashboard summary: counts of clients and projects, and how much money is still owed. Clients
// are billed in different currencies (Flyway V31), so the outstanding figure is kept SEPARATE per
// currency and the currencies are never added together — one `dashboard-outstanding-<CUR>` row per
// currency, each shown in that currency. There is deliberately no single combined total. Each value
// carries the data-testid the test reads.
export function Dashboard() {
  const { data, isPending, isError } = useDashboard();

  if (isPending) {
    return <p data-testid="dashboard-loading">Loading…</p>;
  }
  if (isError) {
    return <p data-testid="dashboard-error">Could not load the dashboard.</p>;
  }

  return (
    <dl data-testid="dashboard">
      <dt>Clients</dt>
      <dd data-testid="dashboard-clients-count">{data.clientsCount}</dd>
      <dt>Jobs</dt>
      <dd data-testid="dashboard-projects-count">{data.projectsCount}</dd>
      {data.outstanding.map((row) => (
        <Fragment key={row.currency}>
          <dt>Outstanding ({row.currency})</dt>
          <dd data-testid={`dashboard-outstanding-${row.currency}`}>
            {formatMoney(row.amount, row.currency)}
          </dd>
        </Fragment>
      ))}
    </dl>
  );
}
