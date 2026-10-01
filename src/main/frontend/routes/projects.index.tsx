import { createFileRoute } from '@tanstack/react-router';
import { ProjectForm } from '../features/projects/ProjectForm';
import { ProjectsList } from '../features/projects/ProjectsList';

// The projects page: one new file under routes/. It composes its own feature's form + list; each
// queries for itself, so this page holds no data and no state.
export const Route = createFileRoute('/projects/')({
  component: ProjectsPage,
});

function ProjectsPage() {
  return (
    <section data-testid="projects-page">
      <h1>Jobs</h1>
      <ProjectForm />
      <ProjectsList />
    </section>
  );
}
