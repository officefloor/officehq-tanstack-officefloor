import { Outlet, createFileRoute } from '@tanstack/react-router';

// The /clients section layout: it only renders whichever child matched (the list at /clients, a
// client's detail at /clients/$clientId). Adding a detail view is a new sibling file under routes/ —
// this layout is written once and not touched again.
export const Route = createFileRoute('/clients')({
  component: () => <Outlet />,
});
