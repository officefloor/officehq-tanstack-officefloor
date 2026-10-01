import { createFileRoute } from '@tanstack/react-router';
import { ClientForm } from '../features/clients/ClientForm';
import { ClientList } from '../features/clients/ClientList';

// The Clients page: one new file under routes/. The route table is generated from this directory.
export const Route = createFileRoute('/clients/')({
  component: ClientsPage,
});

function ClientsPage() {
  return (
    <section data-testid="clients-page">
      <h1>Clients</h1>
      <ClientForm />
      <ClientList />
    </section>
  );
}
