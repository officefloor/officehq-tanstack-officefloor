import { getJson } from '../../api/http';

// An invoice's money summary as the API exposes it: the SUBTOTAL (the sum of its line items), the
// percentage DISCOUNT set on it, the DISCOUNT amount that works out to, and the final TOTAL (subtotal
// minus the discount). The derivation lives on the server (money scale, no float drift). The query
// key shares the ['invoice-lineitems', invoiceId] prefix the line items list uses, so when a line is
// added, changed or removed the same invalidate that refreshes the list also refetches this summary —
// the subtotal and total stay in step with the lines, with no import between the two panels.
export type InvoiceSummary = {
  invoiceId: number;
  subtotal: number;
  discountPct: number;
  discount: number;
  total: number;
};

export const invoiceSummaryKey = (invoiceId: number) =>
  ['invoice-lineitems', invoiceId, 'summary'] as const;

export const getInvoiceSummary = (invoiceId: number): Promise<InvoiceSummary> =>
  getJson<InvoiceSummary>(`/api/invoices/summary?invoiceId=${invoiceId}`);
