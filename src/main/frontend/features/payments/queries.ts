import { getJson, postJson } from '../../api/http';

// An invoice's payments under a per-invoice key: anything showing invoice 1's payments reads
// ['payments', 1], and a write invalidates the same key to refresh them (CLAUDE.md rule 5). A
// payment carries how much the client paid (amount) and when (date, an ISO date string).
export type Payment = {
  id: number;
  invoiceId: number;
  amount: number;
  date: string;
};

export const paymentsKey = (invoiceId: number) => ['payments', invoiceId] as const;

export function fetchPayments(invoiceId: number): Promise<Payment[]> {
  return getJson<Payment[]>(`/api/invoices/${invoiceId}/payments`);
}

/** Record a payment on an invoice; the invoice id is in the path, the rest is the body. */
export function createPayment(
  invoiceId: number,
  input: { amount: number; date: string },
): Promise<Payment> {
  return postJson<Payment>(`/api/invoices/${invoiceId}/payments`, input);
}
