import { createFileRoute } from '@tanstack/react-router';
import { AllInvoicesPage } from '../features/invoices/AllInvoicesPage';

// The /invoices page — one new file under routes/; the route tree is generated from this directory.
// A single leaf route: one place listing every invoice across all projects.
export const Route = createFileRoute('/invoices')({
  component: AllInvoicesPage,
});
