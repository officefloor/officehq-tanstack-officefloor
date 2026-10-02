import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';

// A payment recorded against an invoice: what a client has paid (amount) and when (date, an ISO
// string e.g. "2026-02-01"). Everything that shows an invoice's payments shares the key
// ['payments', invoiceId]; invalidating it after a write refreshes the list.
export type Payment = {
  id: number;
  invoiceId: number;
  amount: number;
  date: string;
};

export const paymentsKey = (invoiceId: number) => ['payments', invoiceId] as const;

export function usePayments(invoiceId: number) {
  return useQuery({
    queryKey: paymentsKey(invoiceId),
    queryFn: () => getJson<Payment[]>(`/api/payments?invoiceId=${invoiceId}`),
  });
}

// Record a payment on an invoice: POSTs {invoiceId, amount, date} and returns the saved row.
// Invalidates ['payments', invoiceId] so the payment list re-reads from the server, and the
// ['invoices'] prefix so anything showing invoices refreshes too — the two stay in step by sharing
// the key, with no import between them.
export function useAddPayment(invoiceId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { amount: number; date: string }) =>
      postJson<Payment>('/api/payments', { invoiceId, ...input }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: paymentsKey(invoiceId) });
      void queryClient.invalidateQueries({ queryKey: ['invoices'] });
    },
  });
}
