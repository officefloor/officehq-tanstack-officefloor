import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ClientDetail } from '../../slots/defs/clientDetail';
import {
  clientContactsKey,
  listClientContacts,
  setPrimaryContact,
  type Contact,
} from './api';

// The client's MAIN contact — one panel filling the client.detail region. Reads the same server
// data as the contacts panel under ['clients', clientId, 'contacts'] (never copied into state); the
// primary flag rides along on each contact, so the current main contact is derived from the list.
// client-primary-contact shows who it is; each contact-primary-<id> button picks a new one. On
// success we invalidate the shared contacts key so this panel (and the contacts table) refetch and
// stay in step — we never hand-maintain the list.
function ClientPrimaryContact({ clientId }: { clientId: number }) {
  const queryClient = useQueryClient();
  const { data: contacts } = useQuery({
    queryKey: clientContactsKey(clientId),
    queryFn: () => listClientContacts(clientId),
  });

  const mutation = useMutation({
    mutationFn: setPrimaryContact,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: clientContactsKey(clientId) });
    },
  });

  if (!contacts) {
    return null;
  }

  const primary = contacts.find((c: Contact) => c.primary);

  return (
    <section>
      <h2>Main contact</h2>
      <p>
        <span data-testid="client-primary-contact">{primary ? primary.name : 'None'}</span>
      </p>
      {contacts.length > 0 && (
        <ul>
          {contacts.map((contact: Contact) => (
            <li key={contact.id}>
              <span>{contact.name}</span>
              <button
                type="button"
                data-testid={`contact-primary-${contact.id}`}
                disabled={contact.primary || mutation.isPending}
                onClick={() => mutation.mutate(contact.id)}
              >
                {contact.primary ? 'Main contact' : 'Make main contact'}
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export const contribution = ClientDetail.fill({ order: 35, Component: ClientPrimaryContact });
