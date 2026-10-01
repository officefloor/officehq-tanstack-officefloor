import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { clientsKey, createClient } from './api';

// A proper email: some local part, an @, a domain, a dot and a TLD — no whitespace. Shared in spirit
// with the server check in CreateClient.java; the DB CHECK constraint is the last line of defence.
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// Add a client. The fields the user is typing live in useState (uncommitted input); the saved data
// lives on the server. Before we call the API we require a proper email — a blank or malformed
// address is refused in the UI (never reaching the server) and surfaced on its own error anchor.
// A duplicate email can only be known server-side (another row may already hold it), so the server
// rejects it and we surface that on the same anchor. On success we invalidate ['clients'] so the
// list refetches — we never hand-maintain the list.
export function ClientForm() {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [emailError, setEmailError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: createClient,
    onSuccess: () => {
      setName('');
      setEmail('');
      void queryClient.invalidateQueries({ queryKey: clientsKey });
    },
    onError: () => {
      setEmailError('That email is already in use.');
    },
  });

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!EMAIL_RE.test(email.trim())) {
      setEmailError('Enter a valid email address.');
      return;
    }
    setEmailError(null);
    mutation.mutate({ name, email: email.trim() });
  };

  return (
    <form onSubmit={submit}>
      <input
        data-testid="client-form-name"
        placeholder="Name"
        value={name}
        onChange={(e) => setName(e.target.value)}
      />
      <input
        data-testid="client-form-email"
        placeholder="Email"
        value={email}
        onChange={(e) => {
          setEmail(e.target.value);
          if (emailError) {
            setEmailError(null);
          }
        }}
      />
      {emailError && (
        <p data-testid="client-form-email-error" role="alert">
          {emailError}
        </p>
      )}
      <button data-testid="client-form-submit" type="submit">
        Add client
      </button>
    </form>
  );
}
