import { createFileRoute } from '@tanstack/react-router';
import { AllInvoiceList } from '../features/invoices/AllInvoiceList';

// The Invoices page: one new file under routes/. Lists every invoice across all projects.
export const Route = createFileRoute('/invoices/')({
  component: InvoicesPage,
});

function InvoicesPage() {
  return (
    <section data-testid="invoices-page">
      <h1>Invoices</h1>
      <AllInvoiceList />
    </section>
  );
}
