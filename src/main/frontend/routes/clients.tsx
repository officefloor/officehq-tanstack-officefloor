import { Outlet, createFileRoute } from '@tanstack/react-router';

// Clients is now a section with detail views, so it gets a one-line layout route rendering <Outlet />:
// the list (clients.index.tsx) and a client's detail (clients.$clientId.tsx) are its children.
// Written once; adding a detail view is a new sibling file, never an edit here (CLAUDE.md rule 1/2).
export const Route = createFileRoute('/clients')({
  component: () => <Outlet />,
});
