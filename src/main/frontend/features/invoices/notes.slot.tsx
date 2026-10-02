import { InvoiceDetail } from '../../slots/defs/invoiceDetail';
import { InvoiceNotes } from './InvoiceNotes';

// The notes panel's presence on the invoice detail page — its own file, filling the shared
// invoice.detail region. The detail route is not touched to add it (CLAUDE.md rule 3). Ordered after
// the payments panel.
export const contribution = InvoiceDetail.fill({ order: 30, Component: InvoiceNotes });
