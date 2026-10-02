import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

// One invoice's money breakdown, worked out on the server (see InvoiceSummaryGetLogic): its subtotal
// (the sum of its line items), the percentage discount taken off it, that discount in money, the
// percentage sales tax, that tax in money, and the final total (subtotal minus the discount, plus
// tax added on top). Keyed under the shared ['invoices'] prefix so adding or removing a line — which
// invalidates ['invoices'] — also refreshes the breakdown, with no import between the two.
export type InvoiceSummary = {
  invoiceId: number;
  subtotal: number;
  discountPct: number;
  discount: number;
  taxPct: number;
  tax: number;
  total: number;
};

export const invoiceSummaryKey = (invoiceId: number) =>
  ['invoices', 'summary', invoiceId] as const;

export function useInvoiceSummary(invoiceId: number) {
  return useQuery({
    queryKey: invoiceSummaryKey(invoiceId),
    queryFn: () => getJson<InvoiceSummary>(`/api/invoices/summary?invoiceId=${invoiceId}`),
  });
}
