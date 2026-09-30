import { InvoiceDetail } from '../../slots/defs/invoiceDetail';
import { InvoiceNotesPanel } from './InvoiceNotesPanel';

// The notes' presence on an invoice's detail page — its own file, filling the invoice-detail slot
// below the payments. The detail route was written once and is not touched to add this; the panel
// queries for its own data (['notes']) scoped to the invoice it is handed.
export const contribution = InvoiceDetail.fill({
  order: 30,
  Component: InvoiceNotesPanel,
});
