import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

// How much each client still owes, read from GET /api/clients/outstanding. The money math lives on
// the server (same rule as the statement); the list reads this to order clients by what they owe.
// Shares the ['clients'] key prefix so it re-reads whenever clients or their invoices change.
export type ClientOutstanding = { clientId: number; outstanding: number };

export function useClientOutstanding() {
  return useQuery({
    queryKey: ['clients', 'outstanding'] as const,
    queryFn: () => getJson<ClientOutstanding[]>('/api/clients/outstanding'),
  });
}
