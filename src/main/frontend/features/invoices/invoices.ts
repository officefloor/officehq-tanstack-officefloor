import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';

// An invoice raised on a project. Everything that shows a project's invoices shares the key
// ['invoices', projectId]; invalidating it after a write refreshes the list and its total together.
export type Invoice = {
  id: number;
  projectId: number;
  amount: number;
  status: string;
  issuedDate: string;
  dueDate: string;
};

export const invoicesKey = (projectId: number) => ['invoices', projectId] as const;

// How a project's invoices are ordered. 'due' asks the server for earliest due date first; any other
// value keeps the default id order. The sort is part of the query key (as a suffix under the shared
// ['invoices', projectId] prefix) so each ordering caches on its own, while a write still invalidates
// the whole prefix and refreshes every ordering at once.
export function useInvoices(projectId: number, sort: string = 'id') {
  return useQuery({
    queryKey: [...invoicesKey(projectId), sort],
    queryFn: () => getJson<Invoice[]>(`/api/invoices?projectId=${projectId}&sort=${sort}`),
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

// Send a draft invoice: POSTs {id} and invalidates ['invoices', projectId] so the list reflects the
// new SENT status from the server. Sending is what records the audit entry and unlocks payment.
export function useSendInvoice(projectId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number }) =>
      postJson<Invoice>('/api/invoices/send', { id: input.id }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: invoicesKey(projectId) }),
  });
}

// Mark an invoice paid: POSTs {id} and invalidates ['invoices', projectId] so the list reflects the
// new status from the server. The server also keeps an audit record of every payment.
export function usePayInvoice(projectId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number }) =>
      postJson<Invoice>('/api/invoices/pay', { id: input.id }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: invoicesKey(projectId) }),
  });
}
