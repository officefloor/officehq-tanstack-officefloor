import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { clientContactsKey, createContact, isValidEmail } from './queries';

// Add a contact to a client. useState holds only what the user is currently typing (CLAUDE.md rule
// 4); the write is a mutation that invalidates ['clients', clientId, 'contacts'] so the table
// refreshes itself — no hand-maintained list. Email is required and must be well-formed (the server
// enforces the same rule) so a bad address can never be saved.
export function ContactForm({ clientId }: { clientId: number }) {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [role, setRole] = useState('');
  const [emailError, setEmailError] = useState(false);
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: createContact,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: clientContactsKey(clientId) });
      setName('');
      setEmail('');
      setRole('');
    },
    onError: () => {
      // Server rejected the address (defence in depth) — surface the same error anchor.
      setEmailError(true);
    },
  });

  return (
    <form
      data-testid="contact-form"
      onSubmit={(event) => {
        event.preventDefault();
        if (!isValidEmail(email)) {
          setEmailError(true);
          return;
        }
        setEmailError(false);
        mutation.mutate({ name, email, role, clientId });
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
      <input
        data-testid="contact-form-role"
        placeholder="Role"
        value={role}
        onChange={(event) => setRole(event.target.value)}
      />
      {emailError && (
        <p data-testid="contact-form-email-error" role="alert">
          Enter a valid email address.
        </p>
      )}
      <button data-testid="contact-form-submit" type="submit">
        Add contact
      </button>
    </form>
  );
}
