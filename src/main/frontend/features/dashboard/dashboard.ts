import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

// The cross-feature summary the home dashboard shows. It shares the ['dashboard'] key; any feature
// that changes a client, project or invoice can invalidate it to keep the counts and total in step.
// Outstanding is kept SEPARATE per currency (clients are billed in different currencies, Flyway V31);
// the currencies are never added together, so there is one figure per currency, not a single total.
export type OutstandingByCurrency = { currency: string; amount: number };

export type DashboardSummary = {
  clientsCount: number;
  projectsCount: number;
  outstanding: OutstandingByCurrency[];
};

export const dashboardKey = ['dashboard'] as const;

export function useDashboard() {
  return useQuery({
    queryKey: dashboardKey,
    queryFn: () => getJson<DashboardSummary>('/api/dashboard'),
  });
}
