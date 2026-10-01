import { createFileRoute } from '@tanstack/react-router';
import { ProjectDetail } from '../slots/defs/projectDetail';

// A project's detail page: a new file under routes/, reached by project-open-<id>. It holds no data
// and no state — it renders the project.detail region with the project id from the URL, and each
// panel (invoices table, total, add form) is its own *.slot.tsx that queries for itself.
export const Route = createFileRoute('/projects/$projectId')({
  component: ProjectDetailPage,
});

function ProjectDetailPage() {
  const { projectId } = Route.useParams();
  return (
    <section data-testid="project-page">
      <h1>Project</h1>
      <ProjectDetail.Slot projectId={Number(projectId)} />
    </section>
  );
}
