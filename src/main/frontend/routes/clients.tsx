import { createFileRoute, Outlet } from '@tanstack/react-router';

// The clients section layout: the one-line route that lets clients have detail views beside the
// list. It renders only <Outlet /> — the list (clients.index.tsx) and a client's detail page
// (clients.$clientId.tsx) are its children, each its own file.
export const Route = createFileRoute('/clients')({
  component: () => <Outlet />,
});
