import { createFileRoute } from '@tanstack/react-router';
import { ProjectForm } from '../features/projects/ProjectForm';
import { ProjectsTable } from '../features/projects/ProjectsTable';

// The Projects page: one new file under routes/ (CLAUDE.md rule 1). The route tree is generated from
// this directory. The page composes the feature's form + list; each queries/mutates for itself.
export const Route = createFileRoute('/projects/')({
  component: ProjectsPage,
});

function ProjectsPage() {
  return (
    <section data-testid="projects-page">
      <ProjectForm />
      <ProjectsTable />
    </section>
  );
}
