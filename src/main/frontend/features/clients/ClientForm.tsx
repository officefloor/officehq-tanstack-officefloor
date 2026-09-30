import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { clientsKey, createClient } from './queries';

// Add a client. useState holds only what the user is currently typing (CLAUDE.md rule 4); the write
// is a mutation that invalidates ['clients'] so the list refreshes itself — no hand-maintained list.
export function ClientForm() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: createClient,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: clientsKey });
      setName('');
      setEmail('');
    },
  });

  return (
    <form
      data-testid="client-form"
      onSubmit={(event) => {
        event.preventDefault();
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
        onChange={(event) => setEmail(event.target.value)}
      />
      <button data-testid="client-form-submit" type="submit">
        Add client
      </button>
    </form>
  );
}
