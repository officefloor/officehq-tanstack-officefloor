import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { money } from '../../ui/money';

// The whole-app summary the server returns: how many clients and projects exist, and the money
// still owed (the sum of every SENT invoice's amount — drafts and paid invoices are excluded).
export type DashboardSummary = {
  clientsCount: number;
  projectsCount: number;
  outstandingTotal: number;
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
        <dt>Projects</dt>
        <dd data-testid="dashboard-projects-count">{data?.projectsCount ?? 0}</dd>
        <dt>Outstanding</dt>
        <dd data-testid="dashboard-outstanding-total">
          {money(Number(data?.outstandingTotal ?? 0))}
        </dd>
      </dl>
    </section>
  );
}
