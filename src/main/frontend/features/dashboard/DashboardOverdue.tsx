import { useOverdue } from './overdue';

// A dashboard tile: how many sent invoices are overdue (past their due date as of the dashboard's
// reference date). Carries the data-testid the test reads.
export function DashboardOverdue() {
  const { data, isPending, isError } = useOverdue();

  if (isPending) {
    return <p data-testid="dashboard-overdue-loading">Loading…</p>;
  }
  if (isError) {
    return <p data-testid="dashboard-overdue-error">Could not load overdue invoices.</p>;
  }

  return (
    <dl data-testid="dashboard-overdue">
      <dt>Overdue invoices</dt>
      <dd data-testid="dashboard-overdue-count">{data.overdueCount}</dd>
    </dl>
  );
}
