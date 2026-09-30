import { createFileRoute } from '@tanstack/react-router';
import { ClientDetail } from '../slots/defs/clientDetail';

// Opening a client is a CHILD ROUTE, not a flag: this new file renders at /clients/$clientId. The
// page itself holds no feature content — it renders the client-detail slot, which the client's
// projects (and anything added later) fill from their own files.
export const Route = createFileRoute('/clients/$clientId')({
  component: ClientDetailPage,
});

function ClientDetailPage() {
  const { clientId } = Route.useParams();
  return (
    <section data-testid="client-detail">
      <ClientDetail.Slot clientId={Number(clientId)} />
    </section>
  );
}
