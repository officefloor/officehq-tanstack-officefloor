import { createFileRoute } from '@tanstack/react-router';
import { AllInvoicesTable } from '../features/invoices/AllInvoicesTable';

// The all-invoices page: one new file under routes/ (CLAUDE.md rule 1). The route tree is generated
// from this directory. A flat page with no detail views, so a single route file (no layout). The
// page composes the feature's list, which queries for itself.
export const Route = createFileRoute('/invoices')({
  component: AllInvoicesPage,
});

function AllInvoicesPage() {
  return (
    <section data-testid="all-invoices-page">
      <AllInvoicesTable />
    </section>
  );
}
