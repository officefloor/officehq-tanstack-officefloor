import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

// The home dashboard's top-clients ranking: the five clients who owe the most, highest first. The
// money math lives on the server (same rule as the statement and the outstanding list). Shares the
// ['dashboard'] key prefix so it re-reads whenever the dashboard's data changes.
export type TopClient = {
  clientId: number;
  name: string;
  outstanding: number;
  // The client's own currency (Flyway V31); their figure is shown in it, never added across currencies.
  currency: string;
};

export const topClientsKey = ['dashboard', 'topClients'] as const;

export function useTopClients() {
  return useQuery({
    queryKey: topClientsKey,
    queryFn: () => getJson<TopClient[]>('/api/dashboard/top-clients'),
  });
}
