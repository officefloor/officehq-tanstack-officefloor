import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { clientsKey, createClient, isValidEmail } from './queries';

// Add a client. useState holds only what the user is currently typing (CLAUDE.md rule 4); the write
// is a mutation that invalidates ['clients'] so the list refreshes itself — no hand-maintained list.
// Email is required and must be well-formed: we block the write client-side (and the server enforces
// the same rule) so a bad address can never be saved.
export function ClientForm() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [emailError, setEmailError] = useState(false);
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: createClient,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: clientsKey });
      setName('');
      setEmail('');
    },
    onError: () => {
      // Server rejected the address (defence in depth) — surface the same error anchor.
      setEmailError(true);
    },
  });

  return (
    <form
      data-testid="client-form"
      onSubmit={(event) => {
        event.preventDefault();
        if (!isValidEmail(email)) {
          setEmailError(true);
          return;
        }
        setEmailError(false);
        mutation.mutate({ name, email });
      }}
    >
      <input
        data-testid="client-form-name"
        placeholder="Name"
        value={name}
        onChange={(event) => setName(event.target.value)}
      />
      <input
        data-testid="client-form-email"
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
        <p data-testid="client-form-email-error" role="alert">
          Enter a valid email address.
        </p>
      )}
      <button data-testid="client-form-submit" type="submit">
        Add client
      </button>
    </form>
  );
}
