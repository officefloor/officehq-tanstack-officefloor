import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

// How much is still left to pay on one invoice: its amount minus everything paid against it, worked
// out on the server (see InvoiceDueGetLogic). Keyed under the shared ['invoices'] prefix so that
// recording a payment — which invalidates ['invoices'] (see useAddPayment) — also refreshes the
// amount due, with no import between the two.
export type InvoiceDue = {
  invoiceId: number;
  amount: number;
  paid: number;
  due: number;
};

export const invoiceDueKey = (invoiceId: number) => ['invoices', 'due', invoiceId] as const;

export function useInvoiceDue(invoiceId: number) {
  return useQuery({
    queryKey: invoiceDueKey(invoiceId),
    queryFn: () => getJson<InvoiceDue>(`/api/invoices/due?invoiceId=${invoiceId}`),
  });
}
