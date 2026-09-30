import { Outlet, createFileRoute } from '@tanstack/react-router';

// The Projects section's layout: it only renders the matched child (the list at /projects or a
// project's detail at /projects/$projectId). Added beside projects.index.tsx because Projects now
// has a detail view (CLAUDE.md rule 1). Drilling in is a child route, never a flag (rule 2).
export const Route = createFileRoute('/projects')({
  component: () => <Outlet />,
});
