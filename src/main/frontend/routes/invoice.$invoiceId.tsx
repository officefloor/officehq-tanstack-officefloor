import { createFileRoute } from '@tanstack/react-router';
import { LineItemForm } from '../features/lineitems/LineItemForm';
import { LineItemsTable } from '../features/lineitems/LineItemsTable';
import { InvoiceDetail } from '../slots/defs/invoiceDetail';

// An invoice's detail page: one new file under routes/ (CLAUDE.md rule 1). Opening an invoice is a
// route, not a flag on the list (rule 2) — the router decides what renders, so the project's invoice
// table is left behind and only the invoice's own detail shows. The page composes the line-item form
// + list; each queries/mutates for itself under the ['lineitems', invoiceId] key.
export const Route = createFileRoute('/invoice/$invoiceId')({
  component: InvoiceDetailPage,
});

function InvoiceDetailPage() {
  const { invoiceId } = Route.useParams();
  const id = Number(invoiceId);
  return (
    <section data-testid="invoice-detail">
      <LineItemForm invoiceId={id} />
      <LineItemsTable invoiceId={id} />
      <InvoiceDetail.Slot invoiceId={id} />
    </section>
  );
}
