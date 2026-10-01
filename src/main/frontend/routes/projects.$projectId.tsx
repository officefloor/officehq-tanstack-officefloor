import { createFileRoute } from '@tanstack/react-router';
import { InvoiceForm } from '../features/invoices/InvoiceForm';
import { InvoiceList } from '../features/invoices/InvoiceList';

// Drilling into a project is its own route — a new file, not a flag on the list (CLAUDE.md rule 2).
// The detail page shows the project's invoices, their total, and a form to raise a new one.
export const Route = createFileRoute('/projects/$projectId')({
  component: ProjectDetailPage,
});

function ProjectDetailPage() {
  const { projectId } = Route.useParams();
  const id = Number(projectId);

  return (
    <section data-testid="project-detail-page">
      <h1>Project</h1>
      <InvoiceForm projectId={id} />
      <InvoiceList projectId={id} />
    </section>
  );
}
