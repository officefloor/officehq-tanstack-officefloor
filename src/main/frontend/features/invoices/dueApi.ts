import { getJson } from '../../api/http';

// How much is still left to pay on one invoice after any payments — what the API exposes for the
// outstanding balance. The query key is scoped to the invoice, ['invoice-due', invoiceId], so each
// row reads (and would invalidate) only its own balance.
export type InvoiceDue = {
  invoiceId: number;
  amountDue: number;
};

// The key for one invoice's outstanding balance. The row's due-amount cell reads it; a successful
// payment invalidates it (shared with the payments key by prefix is not needed — the pay/record
// forms invalidate this key directly) so the balance refetches, with no import between panels.
export const invoiceDueKey = (invoiceId: number) => ['invoice-due', invoiceId] as const;

export const getInvoiceDue = (invoiceId: number): Promise<InvoiceDue> =>
  getJson<InvoiceDue>(`/api/invoices/due?invoiceId=${invoiceId}`);
