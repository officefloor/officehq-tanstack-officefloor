import { getJson, postJson } from '../../api/http';

// A line item as the API exposes it — one thing being charged for on an invoice: a description, how
// many, and the price each. The query key is scoped to the invoice, ['invoice-lineitems', invoiceId],
// so each invoice's detail page reads (and invalidates) only its own lines: the list reads the key,
// the add form invalidates it.
export type LineItem = {
  id: number;
  invoiceId: number;
  description: string;
  qty: number;
  unitPrice: number;
};
export type NewLineItem = {
  invoiceId: number;
  description: string;
  qty: number;
  unitPrice: number;
};

// The key for one invoice's line items. The list query reads it; a successful add invalidates it so
// the list + the derived total refetch, with no import between the two panels.
export const lineItemsKey = (invoiceId: number) => ['invoice-lineitems', invoiceId] as const;

export const listLineItems = (invoiceId: number): Promise<LineItem[]> =>
  getJson<LineItem[]>(`/api/invoices/lineitems?invoiceId=${invoiceId}`);

export const createLineItem = (body: NewLineItem): Promise<LineItem> =>
  postJson<LineItem>('/api/invoices/lineitems', body);

// A line's own amount, and the whole invoice's amount — both DERIVED from qty and unit price, never
// stored on the client. The project's invoices list shows the same figure (the server keeps the
// invoice's stored amount in step as its lines change).
export const lineAmount = (item: LineItem): number => Number(item.qty) * Number(item.unitPrice);

export const invoiceTotal = (items: LineItem[]): number =>
  items.reduce((sum, item) => sum + lineAmount(item), 0);
