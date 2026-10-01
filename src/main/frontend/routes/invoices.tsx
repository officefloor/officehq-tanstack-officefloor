import { Outlet, createFileRoute } from '@tanstack/react-router';

// Invoices is now a section with detail views, so it gets a one-line layout route rendering
// <Outlet />: the all-invoices list (invoices.index.tsx) and a single invoice's detail
// (invoices.$invoiceId.tsx) are its children. Written once; adding a detail view is a new sibling
// file, never an edit here (CLAUDE.md rule 1/2).
export const Route = createFileRoute('/invoices')({
  component: () => <Outlet />,
});
