import { ClientDetail } from '../../slots/defs/clientDetail';
import { ClientContactsTable } from './ClientContactsTable';
import { ContactForm } from './ContactForm';

// The contacts panel on a client's detail page — one new *.slot.tsx file filling the ClientDetail
// region (CLAUDE.md rule 3). Nothing existing is edited to add it. Shows the client's contacts and
// the form to add one; each queries/mutates for itself under the shared contacts key.
function ClientContactsPanel({ clientId }: { clientId: number }) {
  return (
    <section data-testid="client-contacts">
      <ClientContactsTable clientId={clientId} />
      <ContactForm clientId={clientId} />
    </section>
  );
}

export const contribution = ClientDetail.fill({ order: 10, Component: ClientContactsPanel });
