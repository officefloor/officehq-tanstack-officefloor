import { getJson, postJson } from '../../api/http';

// An invoice's line items under a per-invoice key: anything showing invoice 1's lines reads
// ['lineitems', 1], and a write invalidates the same key to refresh them (CLAUDE.md rule 5). A line
// carries what is being charged for (description), how many (qty) and the price each (unitPrice).
export type LineItem = {
  id: number;
  invoiceId: number;
  description: string;
  qty: number;
  unitPrice: number;
};

export const lineItemsKey = (invoiceId: number) => ['lineitems', invoiceId] as const;

export function fetchLineItems(invoiceId: number): Promise<LineItem[]> {
  return getJson<LineItem[]>(`/api/invoices/${invoiceId}/lineitems`);
}

/** Add a line to an invoice; the invoice id is in the path, the rest is the body (CLAUDE.md server). */
export function createLineItem(
  invoiceId: number,
  input: { description: string; qty: number; unitPrice: number },
): Promise<LineItem> {
  return postJson<LineItem>(`/api/invoices/${invoiceId}/lineitems`, input);
}
