import { getJson } from '../../api/http';

// The home dashboard's aggregate, under its own query key. A read-only cross-entity summary: the
// client and project counts and the outstanding (unpaid) total. Anything that changes clients,
// projects or invoices can refresh this by invalidating ['dashboard'] (CLAUDE.md rule 5).
export type DashboardSummary = {
  clients: number;
  projects: number;
  outstanding: number;
  overdue: number;
};

export const dashboardKey = ['dashboard'] as const;

export function fetchDashboardSummary(): Promise<DashboardSummary> {
  return getJson<DashboardSummary>('/api/dashboard/summary');
}

// The dashboard's top clients, ranked by how much they owe (most owed first, at most five). A
// read-only cross-entity view under its own query key; anything that changes clients, projects,
// invoices or payments can refresh it by invalidating ['dashboard', 'top-clients'] (CLAUDE.md
// rule 5).
export type TopClient = {
  clientId: number;
  name: string;
  outstanding: number;
};

export const topClientsKey = ['dashboard', 'top-clients'] as const;

export function fetchTopClients(): Promise<TopClient[]> {
  return getJson<TopClient[]>('/api/dashboard/top-clients');
}
