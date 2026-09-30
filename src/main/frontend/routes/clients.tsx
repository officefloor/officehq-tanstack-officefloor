import { Outlet, createFileRoute } from '@tanstack/react-router';

// The Clients section's layout: it only renders the matched child (the list at /clients or a
// client's detail at /clients/$clientId). Added beside clients.index.tsx because Clients now has a
// detail view (CLAUDE.md rule 1). Drilling in is a child route, never a flag (rule 2).
export const Route = createFileRoute('/clients')({
  component: () => <Outlet />,
});
