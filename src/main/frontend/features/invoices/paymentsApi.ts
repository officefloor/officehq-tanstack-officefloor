import { getJson, postJson } from '../../api/http';

// A payment as the API exposes it — what a client has paid against an invoice: an amount and the
// date it was paid. The query key is scoped to the invoice, ['invoice-payments', invoiceId], so each
// invoice's detail page reads (and invalidates) only its own payments: the list reads the key, the
// record form invalidates it.
export type Payment = {
  id: number;
  invoiceId: number;
  amount: number;
  date: string;
};
export type NewPayment = {
  invoiceId: number;
  amount: number;
  date: string;
};

// The key for one invoice's payments. The list query reads it; a successful record invalidates it so
// the list refetches, with no import between the two panels.
export const paymentsKey = (invoiceId: number) => ['invoice-payments', invoiceId] as const;

export const listPayments = (invoiceId: number): Promise<Payment[]> =>
  getJson<Payment[]>(`/api/invoices/payments?invoiceId=${invoiceId}`);

export const createPayment = (body: NewPayment): Promise<Payment> =>
  postJson<Payment>('/api/invoices/payments', body);
