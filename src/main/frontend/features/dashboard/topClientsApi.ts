import { getJson } from '../../api/http';

// The home screen's "top clients" leaderboard as the API exposes it: each client and how much that
// client still owes, already ranked (most owed first) and capped at five by the server. The query
// key ['dashboard', 'top-clients'] sits under the shared ['dashboard'] handle — any feature that
// writes clients, projects, invoices or payments can invalidate ['dashboard'] to refresh it.
export type TopClient = {
  clientId: number;
  name: string;
  amount: number;
};

export const topClientsKey = ['dashboard', 'top-clients'] as const;

export const getTopClients = (): Promise<TopClient[]> =>
  getJson<TopClient[]>('/api/dashboard/top-clients');
