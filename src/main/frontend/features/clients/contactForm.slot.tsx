import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ClientDetail } from '../../slots/defs/clientDetail';
import { clientContactsKey, createContact } from './api';

// Add a contact (name, email, role) to this client — one panel filling the client.detail region.
// What the user is typing lives in useState (uncommitted input); the saved data lives on the server.
// On success we invalidate ['clients', clientId, 'contacts'] so the contacts panel refetches — we
// never hand-maintain the list.
function ContactForm({ clientId }: { clientId: number }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [role, setRole] = useState('');

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
    if (name.trim() === '' || email.trim() === '' || role.trim() === '') {
      return;
    }
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
        onChange={(e) => setEmail(e.target.value)}
      />
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
