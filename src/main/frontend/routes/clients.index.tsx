import { createFileRoute } from '@tanstack/react-router';
import { ClientForm } from '../features/clients/ClientForm';
import { ClientsTable } from '../features/clients/ClientsTable';

// The Clients page: one new file under routes/ (CLAUDE.md rule 1). The route tree is generated from
// this directory. The page composes the feature's form + list; each queries/mutates for itself.
export const Route = createFileRoute('/clients/')({
  component: ClientsPage,
});

function ClientsPage() {
  return (
    <section data-testid="clients-page">
      <ClientForm />
      <ClientsTable />
    </section>
  );
}
