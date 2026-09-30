import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';

// A contact as the server returns it, including whether it is the client's one main contact.
type Contact = {
  id: number;
  clientId: number;
  name: string;
  email: string;
  role: string;
  primary: boolean;
};

// The client's main contact, shown on the client-detail page: who it currently is, plus a control
// on each contact to make it the main one. Reads the shared ['contacts'] query (rule 5), scoped to
// the client this panel is handed, and never copies it into state. Choosing a contact is a
// useMutation that invalidates ['contacts'], so both this panel and the contacts table refetch.
export function PrimaryContactPanel({ clientId }: { clientId: number }) {
  const queryClient = useQueryClient();
  const contacts = useQuery({
    queryKey: ['contacts'],
    queryFn: () => getJson<Contact[]>('/api/contacts'),
  });

  const setPrimary = useMutation({
    mutationFn: (contactId: number) =>
      postJson<Contact>('/api/contacts/primary', { contactId }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['contacts'] });
    },
  });

  const rows = (contacts.data ?? []).filter((c) => c.clientId === clientId);
  const primary = rows.find((c) => c.primary);

  return (
    <section data-testid="client-primary">
      <p>
        Main contact:{' '}
        <strong data-testid="client-primary-contact">{primary?.name ?? ''}</strong>
      </p>
      <ul>
        {rows.map((c) => (
          <li key={c.id}>
            {c.name}
            {c.primary ? (
              <span data-testid={`contact-primary-current-${c.id}`}> (main)</span>
            ) : (
              <button
                data-testid={`contact-primary-${c.id}`}
                type="button"
                onClick={() => setPrimary.mutate(c.id)}
              >
                Make main
              </button>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}
