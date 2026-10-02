import { InvoiceDetail } from '../../slots/defs/invoiceDetail';
import { useInvoiceDue } from './invoiceDue';

// "What is this invoice's status" — a self-contained panel filling the invoice.detail region. The
// detail route is not edited to add it beyond rendering the region (CLAUDE.md rule 3). The status is
// WORKED OUT from the payments on the server (SENT while unpaid, PARTIAL once part paid, PAID once
// covered), replacing flipping it to paid by hand. Sharing the ['invoices'] query key (via
// useInvoiceDue), it re-reads on its own whenever a payment is recorded — no parent hands it data.
function InvoiceStatus({ invoiceId }: { invoiceId: number }) {
  const { data } = useInvoiceDue(invoiceId);
  return (
    <section data-testid="invoice-status-panel">
      <h2>Status</h2>
      <span data-testid="invoice-status">{data ? data.status : ''}</span>
    </section>
  );
}

export const contribution = InvoiceDetail.fill({ order: 5, Component: InvoiceStatus });
