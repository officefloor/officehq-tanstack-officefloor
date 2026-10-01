import { useQuery } from '@tanstack/react-query';
import { ClientDetail } from '../../slots/defs/clientDetail';
import { clientContactsKey, listClientContacts, type Contact } from './api';

// The contacts a client keeps — one panel filling the client.detail region. Reads server data under
// ['clients', clientId, 'contacts'] (never copied into state); the server scopes to this client, so
// only its contacts arrive. Each row exposes contact-row-<id>/contact-name/contact-role for the
// test contract.
function ClientContacts({ clientId }: { clientId: number }) {
  const { data: contacts } = useQuery({
    queryKey: clientContactsKey(clientId),
    queryFn: () => listClientContacts(clientId),
  });

  if (!contacts) {
    return null;
  }

  if (contacts.length === 0) {
    return <p data-testid="client-contacts-empty">No contacts yet.</p>;
  }

  return (
    <table data-testid="client-contacts-table">
      <thead>
        <tr>
          <th>Name</th>
          <th>Email</th>
          <th>Role</th>
        </tr>
      </thead>
      <tbody>
        {contacts.map((contact: Contact) => (
          <tr key={contact.id} data-testid={`contact-row-${contact.id}`}>
            <td data-testid="contact-name">{contact.name}</td>
            <td data-testid="contact-email">{contact.email}</td>
            <td data-testid="contact-role">{contact.role}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export const contribution = ClientDetail.fill({ order: 30, Component: ClientContacts });
