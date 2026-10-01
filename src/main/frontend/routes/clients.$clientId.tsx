import { createFileRoute } from '@tanstack/react-router';
import { ClientDetail } from '../slots/defs/clientDetail';

// A client's detail page: a new file under routes/, reached by client-open-<id>. It holds no data
// and no state — it renders the client.detail region with the client id from the URL, and each
// panel (the client's projects, and whatever later joins) is its own *.slot.tsx that queries for
// itself.
export const Route = createFileRoute('/clients/$clientId')({
  component: ClientDetailPage,
});

function ClientDetailPage() {
  const { clientId } = Route.useParams();
  return (
    <section data-testid="client-page">
      <h1>Client</h1>
      <ClientDetail.Slot clientId={Number(clientId)} />
    </section>
  );
}
