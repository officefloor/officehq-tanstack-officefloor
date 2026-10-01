import { createFileRoute } from '@tanstack/react-router';
import { ClientProjects } from '../features/clients/ClientProjects';

// Drilling into a client is its own route — a new file, not a flag on the list (CLAUDE.md rule 2).
// The detail page shows the projects the owner is doing for that client.
export const Route = createFileRoute('/clients/$clientId')({
  component: ClientDetailPage,
});

function ClientDetailPage() {
  const { clientId } = Route.useParams();
  const id = Number(clientId);

  return (
    <section data-testid="client-detail-page">
      <h1>Client</h1>
      <ClientProjects clientId={id} />
    </section>
  );
}
