import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { money } from '../../ui/money';
import { DashboardPanels } from '../../slots/defs/dashboardPanels';

// The whole-app summary the server returns: how many clients and projects exist, and the money still
// owed (the sum of every SENT invoice's amount — drafts and paid invoices are excluded). Clients are
// billed in different currencies, so the money owed is kept SEPARATE per currency, keyed by currency
// code (e.g. { USD: 100, EUR: 200 }) and never added together.
export type DashboardSummary = {
  clientsCount: number;
  projectsCount: number;
  outstandingByCurrency: Record<string, number>;
  overdueCount: number;
};

// The home screen: a read-only dashboard of the counts and the outstanding total. Server data is
// read with useQuery under the ['dashboard'] key — never copied into state. A write elsewhere that
// invalidates ['clients'], ['projects'] or ['invoices'] does not touch this key, so the dashboard
// simply refetches on its own next mount; it stays a single source of truth for the summary.
export function Dashboard() {
  const summary = useQuery({
    queryKey: ['dashboard'],
    queryFn: () => getJson<DashboardSummary>('/api/dashboard'),
  });

  const data = summary.data;

  return (
    <section data-testid="dashboard">
      <dl>
        <dt>Clients</dt>
        <dd data-testid="dashboard-clients-count">{data?.clientsCount ?? 0}</dd>
        <dt>Jobs</dt>
        <dd data-testid="dashboard-projects-count">{data?.projectsCount ?? 0}</dd>
        <dt>Outstanding</dt>
        {Object.entries(data?.outstandingByCurrency ?? {}).map(([currency, amount]) => (
          <dd key={currency} data-testid={`dashboard-outstanding-${currency}`}>
            {money(Number(amount), currency)}
          </dd>
        ))}
        <dt>Overdue invoices</dt>
        <dd data-testid="dashboard-overdue-count">{data?.overdueCount ?? 0}</dd>
      </dl>
      <DashboardPanels.Slot />
    </section>
  );
}
