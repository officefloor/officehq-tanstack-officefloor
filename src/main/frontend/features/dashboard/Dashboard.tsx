import { useDashboard } from './dashboard';

// The dashboard summary: counts of clients and projects, and how much money is still owed (the sum of
// UNPAID invoice amounts). Each value carries the data-testid the test reads; the outstanding total is
// rendered to two decimals to match a money amount.
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
      <dt>Projects</dt>
      <dd data-testid="dashboard-projects-count">{data.projectsCount}</dd>
      <dt>Outstanding</dt>
      <dd data-testid="dashboard-outstanding-total">{data.outstandingTotal.toFixed(2)}</dd>
    </dl>
  );
}
