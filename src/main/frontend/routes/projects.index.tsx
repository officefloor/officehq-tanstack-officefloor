import { createFileRoute } from '@tanstack/react-router';
import { ProjectsPage } from '../features/projects/ProjectsPage';

// The /projects page — one new file under routes/; the route tree is generated from this directory.
export const Route = createFileRoute('/projects/')({
  component: ProjectsPage,
});
