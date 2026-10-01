import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

// The cross-feature summary the home dashboard shows. It shares the ['dashboard'] key; any feature
// that changes a client, project or invoice can invalidate it to keep the counts and total in step.
export type DashboardSummary = {
  clientsCount: number;
  projectsCount: number;
  outstandingTotal: number;
};

export const dashboardKey = ['dashboard'] as const;

export function useDashboard() {
  return useQuery({
    queryKey: dashboardKey,
    queryFn: () => getJson<DashboardSummary>('/api/dashboard'),
  });
}
