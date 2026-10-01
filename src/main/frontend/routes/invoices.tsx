import { createFileRoute } from '@tanstack/react-router';
import { AllInvoices } from '../features/invoices/AllInvoices';

// The invoices page: one new file under routes/. A leaf page with no detail views, so it is a flat
// route composing the one list of every invoice across all projects, which queries for itself.
export const Route = createFileRoute('/invoices')({
  component: InvoicesPage,
});

function InvoicesPage() {
  return (
    <section data-testid="invoices-page">
      <h1>Invoices</h1>
      <AllInvoices />
    </section>
  );
}
