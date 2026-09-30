import { createFileRoute } from '@tanstack/react-router';
import { InvoiceDetail } from '../slots/defs/invoiceDetail';

// Opening an invoice is a CHILD ROUTE, not a flag: this new file renders at /invoice/$invoiceId. The
// page itself holds no feature content — it renders the invoice-detail slot, which the invoice's
// line items (and anything added later) fill from their own files.
export const Route = createFileRoute('/invoice/$invoiceId')({
  component: InvoiceDetailPage,
});

function InvoiceDetailPage() {
  const { invoiceId } = Route.useParams();
  return (
    <section data-testid="invoice-detail">
      <InvoiceDetail.Slot invoiceId={Number(invoiceId)} />
    </section>
  );
}
