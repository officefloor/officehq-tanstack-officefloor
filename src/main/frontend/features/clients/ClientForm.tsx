import { useState } from 'react';
import { useCreateClient } from './clients';

// Add-a-client form. useState holds only what the user is currently typing; on submit it POSTs and
// invalidates the ['clients'] key, so the list refetches itself.
export function ClientForm() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const create = useCreateClient();

  return (
    <form
      data-testid="client-form"
      onSubmit={(event) => {
        event.preventDefault();
        create.mutate(
          { name, email },
          {
            onSuccess: () => {
              setName('');
              setEmail('');
            },
          },
        );
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
