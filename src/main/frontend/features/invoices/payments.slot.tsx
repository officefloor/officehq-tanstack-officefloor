import { InvoiceDetail } from '../../slots/defs/invoiceDetail';
import { InvoicePaymentsPanel } from './InvoicePaymentsPanel';

// The payments' presence on an invoice's detail page — its own file, filling the invoice-detail
// slot below the line items. The detail route was written once and is not touched to add this; the
// panel queries for its own data (['payments']) scoped to the invoice it is handed.
export const contribution = InvoiceDetail.fill({
  order: 20,
  Component: InvoicePaymentsPanel,
});
