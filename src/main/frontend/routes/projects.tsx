import { Outlet, createFileRoute } from '@tanstack/react-router';

// The /projects section layout: it only renders whichever child matched (the list at /projects, a
// project's detail at /projects/$projectId). Adding a detail view is a new sibling file under
// routes/ — this layout is written once and not touched again.
export const Route = createFileRoute('/projects')({
  component: () => <Outlet />,
});
