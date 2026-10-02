import { createFileRoute } from '@tanstack/react-router';
import { InvoiceForm } from '../features/invoices/InvoiceForm';
import { InvoiceList } from '../features/invoices/InvoiceList';
import { ProjectDetail } from '../slots/defs/projectDetail';

// Drilling into a project is its own route — a new file, not a flag on the list (CLAUDE.md rule 2).
// The detail page shows the project's invoices, their total, and a form to raise a new one. Panels
// about the project (its task list, and more over time) fill the project.detail slot region, each in
// its own *.slot.tsx file — this route renders the region and never lists what goes in it.
export const Route = createFileRoute('/projects/$projectId')({
  component: ProjectDetailPage,
});

function ProjectDetailPage() {
  const { projectId } = Route.useParams();
  const id = Number(projectId);

  return (
    <section data-testid="project-detail-page">
      <h1>Job</h1>
      <ProjectDetail.Slot projectId={id} />
      <InvoiceForm projectId={id} />
      <InvoiceList projectId={id} />
    </section>
  );
}
