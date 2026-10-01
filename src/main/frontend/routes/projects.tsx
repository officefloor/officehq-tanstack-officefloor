import { Outlet, createFileRoute } from '@tanstack/react-router';

// Projects is a section with detail views, so it gets a one-line layout route rendering <Outlet />:
// the list (projects.index.tsx) and a project's detail (projects.$projectId.tsx) are its children.
// Written once; adding a detail view is a new sibling file, never an edit here (CLAUDE.md rule 1/2).
export const Route = createFileRoute('/projects')({
  component: () => <Outlet />,
});
