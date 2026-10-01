import { getJson } from '../../api/http';

// The home dashboard summary as the API exposes it: the two counts, what is outstanding kept
// SEPARATE per currency (clients are billed in different currencies and their money is never added
// together) and the overdue count (SENT invoices past their due date). The query key ['dashboard']
// is the shared handle — any feature that writes clients, projects or invoices can invalidate it to
// refresh the home figures.
export type CurrencyAmount = { currency: string; amount: number };

export type Dashboard = {
  clients: number;
  projects: number;
  outstandingByCurrency: CurrencyAmount[];
  overdue: number;
};

export const dashboardKey = ['dashboard'] as const;

export const getDashboard = (): Promise<Dashboard> => getJson<Dashboard>('/api/dashboard');
