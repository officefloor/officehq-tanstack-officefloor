import { createFileRoute } from '@tanstack/react-router';
import { ClientForm } from '../features/clients/ClientForm';
import { ClientsList } from '../features/clients/ClientsList';

// The clients page: one new file under routes/. It composes its own feature's form + list; each
// queries for itself, so this page holds no data and no state.
export const Route = createFileRoute('/clients/')({
  component: ClientsPage,
});

function ClientsPage() {
  return (
    <section data-testid="clients-page">
      <h1>Clients</h1>
      <ClientForm />
      <ClientsList />
    </section>
  );
}
