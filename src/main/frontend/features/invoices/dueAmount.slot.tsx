import { InvoiceRow } from '../../slots/defs/invoiceRow';
import { formatMoney } from '../../ui/money';
import type { Invoice } from './invoices';
import { useInvoiceDue } from './invoiceDue';

// "How much is still left to pay on this invoice" — a self-contained cell filling the invoice-row
// region. The table is not edited to add it beyond rendering the region (CLAUDE.md rule 3); it reads
// the server-derived amount due (invoice amount minus its payments) for its own invoice. Sharing the
// ['invoices'] query key, it drops on its own when a payment is recorded — no parent hands it data.
function DueAmount({ invoice }: { invoice: Invoice }) {
  const { data } = useInvoiceDue(invoice.id);
  return (
    <span data-testid="invoice-due-amount">{data ? formatMoney(data.due) : ''}</span>
  );
}

export const contribution = InvoiceRow.fill({ order: 10, Component: DueAmount });
