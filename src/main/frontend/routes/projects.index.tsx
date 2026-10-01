import { createFileRoute } from '@tanstack/react-router';
import { ProjectForm } from '../features/projects/ProjectForm';
import { ProjectList } from '../features/projects/ProjectList';

// The Projects page: one new file under routes/. The route table is generated from this directory.
export const Route = createFileRoute('/projects/')({
  component: ProjectsPage,
});

function ProjectsPage() {
  return (
    <section data-testid="projects-page">
      <h1>Projects</h1>
      <ProjectForm />
      <ProjectList />
    </section>
  );
}
