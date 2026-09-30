import { getJson } from '../../api/http';

// The home dashboard's aggregate, under its own query key. A read-only cross-entity summary: the
// client and project counts and the outstanding (unpaid) total. Anything that changes clients,
// projects or invoices can refresh this by invalidating ['dashboard'] (CLAUDE.md rule 5).
export type DashboardSummary = {
  clients: number;
  projects: number;
  outstanding: number;
};

export const dashboardKey = ['dashboard'] as const;

export function fetchDashboardSummary(): Promise<DashboardSummary> {
  return getJson<DashboardSummary>('/api/dashboard/summary');
}
