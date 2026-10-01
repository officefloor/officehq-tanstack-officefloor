import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';

// An invoice raised on a project. Everything that shows a project's invoices shares the key
// ['invoices', projectId]; invalidating it after a write refreshes the list and its total together.
export type Invoice = { id: number; projectId: number; amount: number };

export const invoicesKey = (projectId: number) => ['invoices', projectId] as const;

export function useInvoices(projectId: number) {
  return useQuery({
    queryKey: invoicesKey(projectId),
    queryFn: () => getJson<Invoice[]>(`/api/invoices?projectId=${projectId}`),
  });
}

export function useCreateInvoice(projectId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { amount: number }) =>
      postJson<Invoice>('/api/invoices', { projectId, amount: input.amount }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: invoicesKey(projectId) }),
  });
}
