import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { clientsKey, createClient } from './api';

// Add a client. The fields the user is typing live in useState (uncommitted input); the saved data
// lives on the server. On success we invalidate ['clients'] so the list refetches — we never
// hand-maintain the list after the write.
export function ClientForm() {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');

  const mutation = useMutation({
    mutationFn: createClient,
    onSuccess: () => {
      setName('');
      setEmail('');
      void queryClient.invalidateQueries({ queryKey: clientsKey });
    },
  });

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    mutation.mutate({ name, email });
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
        onChange={(e) => setEmail(e.target.value)}
      />
      <button data-testid="client-form-submit" type="submit">
        Add client
      </button>
    </form>
  );
}
