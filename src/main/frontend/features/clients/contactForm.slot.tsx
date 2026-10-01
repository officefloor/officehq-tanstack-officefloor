import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ClientDetail } from '../../slots/defs/clientDetail';
import { clientContactsKey, createContact } from './api';

// A proper email: some local part, an @, a domain, a dot and a TLD — no whitespace. The same check
// clients use (ClientForm.tsx); the server (CreateContact.java) and the DB CHECK constraint in
// V12__contact_email_format.sql are the further lines of defence.
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// Add a contact (name, email, role) to this client — one panel filling the client.detail region.
// What the user is typing lives in useState (uncommitted input); the saved data lives on the server.
// Before we call the API we require a proper email — a blank or malformed address is refused in the
// UI (never reaching the server) and surfaced on its own error anchor. On success we invalidate
// ['clients', clientId, 'contacts'] so the contacts panel refetches — we never hand-maintain the list.
function ContactForm({ clientId }: { clientId: number }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [role, setRole] = useState('');
  const [emailError, setEmailError] = useState(false);

  const mutation = useMutation({
    mutationFn: createContact,
    onSuccess: () => {
      setName('');
      setEmail('');
      setRole('');
      void queryClient.invalidateQueries({ queryKey: clientContactsKey(clientId) });
    },
  });

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (name.trim() === '' || role.trim() === '') {
      return;
    }
    if (!EMAIL_RE.test(email.trim())) {
      setEmailError(true);
      return;
    }
    setEmailError(false);
    mutation.mutate({ clientId, name: name.trim(), email: email.trim(), role: role.trim() });
  };

  return (
    <form onSubmit={submit}>
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
        onChange={(e) => {
          setEmail(e.target.value);
          if (emailError) {
            setEmailError(false);
          }
        }}
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
  );
}

export const contribution = ClientDetail.fill({ order: 40, Component: ContactForm });
