import { createFileRoute, Outlet } from '@tanstack/react-router';

// The projects section layout: the one-line route that lets projects have detail views beside the
// list. It renders only <Outlet /> — the list (projects.index.tsx) and a project's detail page
// (projects.$projectId.tsx) are its children, each its own file.
export const Route = createFileRoute('/projects')({
  component: () => <Outlet />,
});
