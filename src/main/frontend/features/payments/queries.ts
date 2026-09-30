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

// One line of a split payment: how much of the lump sum is applied to a single invoice. The form
// builds one per open invoice the client puts money toward.
export type PaymentAllocation = { invoiceId: number; amount: number };

// The URL search-param key the "record a payment" control on a client's detail page owns and the
// panel reads (CLAUDE.md rule 4): whether the split-payment form is open outlives a click.
export const CLIENT_PAYMENT_PARAM = 'recordPayment';

/**
 * Record ONE lump payment a client made and split it across several of their open invoices: the
 * client id is in the path, {amount, date, allocations} in the body. The server inserts one payment
 * row per allocation, so each invoice's balance then reflects its share. Returns the saved rows.
 */
export function createClientPayment(
  clientId: number,
  input: { amount: number; date: string; allocations: PaymentAllocation[] },
): Promise<Payment[]> {
  return postJson<Payment[]>(`/api/clients/${clientId}/payments`, input);
}
