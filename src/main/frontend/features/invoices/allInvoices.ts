import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

// Every invoice across all projects, each joined to the NAME of the project it bills and its stage
// (status). Served already in id order by GET /api/invoices/all. Its own query key, distinct from the
// per-project ['invoices', projectId] lists.
export type AllInvoice = {
  id: number;
  projectId: number;
  projectName: string;
  amount: number;
  status: string;
};

export const allInvoicesKey = ['invoices', 'all'] as const;

export function useAllInvoices() {
  return useQuery({
    queryKey: allInvoicesKey,
    queryFn: () => getJson<AllInvoice[]>('/api/invoices/all'),
  });
}
