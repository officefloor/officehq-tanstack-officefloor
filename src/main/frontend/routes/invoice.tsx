import { createFileRoute, Outlet } from '@tanstack/react-router';

// The invoice section layout: the one-line route that lets an invoice have a detail view. It renders
// only <Outlet /> — an invoice's detail page (invoice.$invoiceId.tsx) is its child, its own file.
// There is no /invoice list (invoices are listed under their project), so this layout has no index.
export const Route = createFileRoute('/invoice')({
  component: () => <Outlet />,
});
