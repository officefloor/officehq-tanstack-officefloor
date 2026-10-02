import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

// A client's statement, read from GET /api/clients/statement: every invoice across the client's
// projects in one place, each with how much is still due, plus the outstanding total still owed.
// Its own query key, scoped to the one client.
export type StatementInvoice = {
  id: number;
  projectId: number;
  amount: number;
  paid: number;
  due: number;
  status: string;
};

export type ClientStatement = {
  clientId: number;
  invoices: StatementInvoice[];
  outstandingTotal: number;
};

export function useClientStatement(clientId: number) {
  return useQuery({
    queryKey: ['clients', clientId, 'statement'] as const,
    queryFn: () => getJson<ClientStatement>(`/api/clients/statement?clientId=${clientId}`),
  });
}
