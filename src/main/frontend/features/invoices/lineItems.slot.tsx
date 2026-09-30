import { InvoiceDetail } from '../../slots/defs/invoiceDetail';
import { InvoiceLineItemsPanel } from './InvoiceLineItemsPanel';

// The line items' presence on an invoice's detail page — its own file, filling the invoice-detail
// slot. The detail route was written once and is not touched to add this; the panel queries for its
// own data (['lineItems']) scoped to the invoice it is handed.
export const contribution = InvoiceDetail.fill({
  order: 10,
  Component: InvoiceLineItemsPanel,
});
