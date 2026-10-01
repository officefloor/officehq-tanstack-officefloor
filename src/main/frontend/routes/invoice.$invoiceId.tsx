import { createFileRoute } from '@tanstack/react-router';
import { InvoiceDetail } from '../slots/defs/invoiceDetail';

// An invoice's detail page: a new file under routes/, reached by invoice-open-<id> from the project's
// invoices list. It holds no data and no state — it renders the invoice.detail region with the
// invoice id from the URL, and each panel (the line items table + total, the add-line form) is its
// own *.slot.tsx that queries for itself.
export const Route = createFileRoute('/invoice/$invoiceId')({
  component: InvoiceDetailPage,
});

function InvoiceDetailPage() {
  const { invoiceId } = Route.useParams();
  return (
    <section data-testid="invoice-page">
      <h1>Invoice</h1>
      <InvoiceDetail.Slot invoiceId={Number(invoiceId)} />
    </section>
  );
}
