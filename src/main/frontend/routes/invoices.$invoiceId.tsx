import { createFileRoute } from '@tanstack/react-router';
import { LineItemForm } from '../features/invoices/LineItemForm';
import { LineItemList } from '../features/invoices/LineItemList';

// Drilling into an invoice is its own route — a new file, not a flag on the list (CLAUDE.md rule 2).
// The detail page lists the invoice's line items (what is being charged for), their worked-out
// total, and a form to add another line. Each panel reads its own query key and derives the total
// from the rows — this route renders them and never holds the data itself.
export const Route = createFileRoute('/invoices/$invoiceId')({
  component: InvoiceDetailPage,
});

function InvoiceDetailPage() {
  const { invoiceId } = Route.useParams();
  const id = Number(invoiceId);

  return (
    <section data-testid="invoice-detail-page">
      <h1>Invoice</h1>
      <LineItemList invoiceId={id} />
      <LineItemForm invoiceId={id} />
    </section>
  );
}
