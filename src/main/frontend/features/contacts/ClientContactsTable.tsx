import { useQuery } from '@tanstack/react-query';
import { clientContactsKey, fetchClientContacts, type Contact } from './queries';

// The contacts kept for one client. Queries for itself under ['clients', clientId, 'contacts'] —
// never handed its data by a parent (CLAUDE.md rule 5). Carries the stable data-testid contract:
// the table, a row per contact, and the name/email/role cells the test reads.
export function ClientContactsTable({ clientId }: { clientId: number }) {
  const { data: contacts } = useQuery({
    queryKey: clientContactsKey(clientId),
    queryFn: () => fetchClientContacts(clientId),
  });

  if (!contacts) {
    return null;
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
