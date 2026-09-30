import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ClientDetail } from '../../slots/defs/clientDetail';
import {
  clientContactsKey,
  fetchClientContacts,
  setPrimaryContact,
  type Contact,
} from './queries';

// The main-contact panel on a client's detail page — one new *.slot.tsx file filling the ClientDetail
// region (CLAUDE.md rule 3); nothing existing is edited. It reads the client's contacts under the
// SHARED ['clients', clientId, 'contacts'] key (rule 5), shows who the main contact is, and offers a
// "make main" button per contact. Choosing one is a mutation that invalidates the same key, so this
// panel and the contacts table both refresh — no hand-maintained state. Carries the stable testids:
// client-primary-contact (the name shown) and contact-primary-<id> (the choose button per contact).
function PrimaryContactPanel({ clientId }: { clientId: number }) {
  const queryClient = useQueryClient();

  const { data: contacts } = useQuery({
    queryKey: clientContactsKey(clientId),
    queryFn: () => fetchClientContacts(clientId),
  });

  const choose = useMutation({
    mutationFn: (contactId: number) => setPrimaryContact(clientId, contactId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: clientContactsKey(clientId) });
    },
  });

  if (!contacts) {
    return null;
  }

  const primary = contacts.find((contact: Contact) => contact.primary);

  return (
    <section data-testid="client-primary">
      <p>
        Main contact:{' '}
        <span data-testid="client-primary-contact">{primary ? primary.name : ''}</span>
      </p>
      <ul>
        {contacts.map((contact: Contact) => (
          <li key={contact.id}>
            <button
              type="button"
              data-testid={`contact-primary-${contact.id}`}
              onClick={() => choose.mutate(contact.id)}
            >
              Make {contact.name} the main contact
            </button>
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = ClientDetail.fill({ order: 5, Component: PrimaryContactPanel });
