import { useContacts, useSetPrimaryContact, type Contact } from './contacts';

// The "main contact" panel on a client's page: who the client's one main (primary) contact is, and
// a control per contact to change it. Contacts are read from the shared ['contacts'] key and
// filtered to this client (CLAUDE.md rule 5) — never copied into state. Picking a main contact POSTs
// through the shared mutation, which invalidates ['contacts'] so this panel (and anything else
// showing contacts) re-reads and the shown main contact updates itself.
export function PrimaryContact({ clientId }: { clientId: number }) {
  const { data: contacts } = useContacts();
  const setPrimary = useSetPrimaryContact();

  const forClient: Contact[] = (contacts ?? []).filter((c) => c.clientId === clientId);
  const main = forClient.find((c) => c.primary);

  return (
    <section data-testid="client-main-contact">
      <h2>Main contact</h2>
      <p data-testid="client-primary-contact">{main ? main.name : ''}</p>
      <ul data-testid="client-main-contact-choices">
        {forClient.map((contact) => (
          <li key={contact.id}>
            {contact.name}
            <button
              type="button"
              data-testid={`contact-primary-${contact.id}`}
              disabled={setPrimary.isPending || contact.primary}
              onClick={() => setPrimary.mutate(contact.id)}
            >
              Make main contact
            </button>
          </li>
        ))}
      </ul>
    </section>
  );
}
