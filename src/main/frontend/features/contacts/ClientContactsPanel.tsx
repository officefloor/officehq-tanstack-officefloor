import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';

// A contact as the server returns it: name, email and role, plus the id of the client it belongs to.
export type Contact = {
  id: number;
  clientId: number;
  name: string;
  email: string;
  role: string;
};

// A contact must carry a proper email address. Same shape the server enforces (ContactsPost) so the
// UI never asks the server to save what the server will reject.
export function isValidEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
}

// A client's contacts, rendered in the client-detail context: the list of that client's contacts
// plus a form to add one. Server data is read with useQuery under the ['contacts'] key and filtered
// to the client this panel is handed — never copied into state, never hand-maintained (rule 5). A
// create is a useMutation that invalidates ['contacts'], so the list refetches itself. The only
// useState here is the three fields the user is currently typing into (rule 4).
export function ClientContactsPanel({ clientId }: { clientId: number }) {
  const queryClient = useQueryClient();
  const contacts = useQuery({
    queryKey: ['contacts'],
    queryFn: () => getJson<Contact[]>('/api/contacts'),
  });

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [role, setRole] = useState('');
  const [emailError, setEmailError] = useState(false);

  const create = useMutation({
    mutationFn: () =>
      postJson<Contact>('/api/contacts', { clientId, name, email, role }),
    onSuccess: () => {
      setName('');
      setEmail('');
      setRole('');
      setEmailError(false);
      void queryClient.invalidateQueries({ queryKey: ['contacts'] });
    },
  });

  const rows = (contacts.data ?? []).filter((c) => c.clientId === clientId);

  return (
    <section data-testid="client-contacts">
      <form
        data-testid="contact-form"
        onSubmit={(e) => {
          e.preventDefault();
          if (!name.trim() || !role.trim()) {
            return;
          }
          if (!isValidEmail(email)) {
            setEmailError(true);
            return;
          }
          setEmailError(false);
          create.mutate();
        }}
      >
        <input
          data-testid="contact-form-name"
          placeholder="Name"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <input
          data-testid="contact-form-email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        {emailError && (
          <p data-testid="contact-form-email-error" role="alert">
            Enter a valid email address.
          </p>
        )}
        <input
          data-testid="contact-form-role"
          placeholder="Role"
          value={role}
          onChange={(e) => setRole(e.target.value)}
        />
        <button data-testid="contact-form-submit" type="submit">
          Add contact
        </button>
      </form>

      {rows.length === 0 ? (
        <p data-testid="client-contacts-empty">No contacts yet.</p>
      ) : (
        <table data-testid="client-contacts-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((c) => (
              <tr key={c.id} data-testid={`contact-row-${c.id}`}>
                <td data-testid="contact-name">{c.name}</td>
                <td data-testid="contact-email">{c.email}</td>
                <td data-testid="contact-role">{c.role}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
