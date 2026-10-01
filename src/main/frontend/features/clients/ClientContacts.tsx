import { useState } from 'react';
import { isValidEmail } from './email';
import { useContacts, useCreateContact, type Contact } from './contacts';

// The contacts panel on a client's page: the client's contacts in a table, and a form to add one.
// Server data is read from the shared ['contacts'] key and filtered to this client (CLAUDE.md
// rule 5) — never copied into state. The form's useState holds only what the user is currently
// typing; on submit it POSTs {clientId, name, email, role} and invalidates the key, so the table
// refreshes itself.
export function ClientContacts({ clientId }: { clientId: number }) {
  const { data: contacts } = useContacts();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [role, setRole] = useState('');
  const [emailError, setEmailError] = useState(false);
  const create = useCreateContact();

  const forClient: Contact[] = (contacts ?? []).filter((c) => c.clientId === clientId);

  return (
    <section data-testid="client-contacts">
      <h2>Contacts</h2>
      <table data-testid="client-contacts-table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Email</th>
            <th>Role</th>
          </tr>
        </thead>
        <tbody>
          {forClient.map((contact) => (
            <tr key={contact.id} data-testid={`contact-row-${contact.id}`}>
              <td data-testid="contact-name">{contact.name}</td>
              <td data-testid="contact-email">{contact.email}</td>
              <td data-testid="contact-role">{contact.role}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <form
        data-testid="contact-form"
        onSubmit={(event) => {
          event.preventDefault();
          if (!name.trim() || !role.trim()) {
            return;
          }
          if (!isValidEmail(email)) {
            setEmailError(true);
            return;
          }
          setEmailError(false);
          create.mutate(
            { clientId, name, email, role },
            {
              onSuccess: () => {
                setName('');
                setEmail('');
                setRole('');
              },
            },
          );
        }}
      >
        <input
          data-testid="contact-form-name"
          placeholder="Name"
          value={name}
          onChange={(event) => setName(event.target.value)}
        />
        <input
          data-testid="contact-form-email"
          placeholder="Email"
          value={email}
          onChange={(event) => {
            setEmail(event.target.value);
            if (emailError) {
              setEmailError(false);
            }
          }}
        />
        {emailError && (
          <span data-testid="contact-form-email-error" role="alert">
            Enter a valid email address.
          </span>
        )}
        <input
          data-testid="contact-form-role"
          placeholder="Role"
          value={role}
          onChange={(event) => setRole(event.target.value)}
        />
        <button data-testid="contact-form-submit" type="submit">
          Add contact
        </button>
      </form>
    </section>
  );
}
