import { createFileRoute } from '@tanstack/react-router';
import { ClientProjectsTable } from '../features/clients/ClientProjectsTable';

// A client's detail page: one new file under routes/ (CLAUDE.md rule 1). Opening a client is a child
// route, not a flag on the list (rule 2). It shows the projects the user is doing for that client;
// the table queries for itself under the ['clients', clientId, 'projects'] key.
export const Route = createFileRoute('/clients/$clientId')({
  component: ClientDetailPage,
});

function ClientDetailPage() {
  const { clientId } = Route.useParams();
  const id = Number(clientId);
  return (
    <section data-testid="client-detail">
      <ClientProjectsTable clientId={id} />
    </section>
  );
}
