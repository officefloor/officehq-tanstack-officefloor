import { createFileRoute } from '@tanstack/react-router';
import { InvoiceForm } from '../features/invoices/InvoiceForm';
import { InvoicesTable } from '../features/invoices/InvoicesTable';

// A project's detail page: one new file under routes/ (CLAUDE.md rule 1). Opening a project is a
// child route, not a flag on the list (rule 2). The page composes the invoices form + list; each
// queries/mutates for itself under the ['invoices', projectId] key.
export const Route = createFileRoute('/projects/$projectId')({
  component: ProjectDetailPage,
});

function ProjectDetailPage() {
  const { projectId } = Route.useParams();
  const id = Number(projectId);
  return (
    <section data-testid="project-detail">
      <InvoiceForm projectId={id} />
      <InvoicesTable projectId={id} />
    </section>
  );
}
