import { createFileRoute } from '@tanstack/react-router';
import { ProjectDetail } from '../slots/defs/projectDetail';

// Opening a project is a CHILD ROUTE, not a flag: this new file renders at /projects/$projectId.
// The page itself holds no feature content — it renders the project-detail slot, which the project's
// invoices (and anything added later) fill from their own files.
export const Route = createFileRoute('/projects/$projectId')({
  component: ProjectDetailPage,
});

function ProjectDetailPage() {
  const { projectId } = Route.useParams();
  return (
    <section data-testid="project-detail">
      <ProjectDetail.Slot projectId={Number(projectId)} />
    </section>
  );
}
