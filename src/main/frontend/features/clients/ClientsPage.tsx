import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';

// A client as the server returns it.
export type Client = { id: number; name: string; email: string };

// A client must carry a proper email address. Same shape the server enforces (ClientsPost) so the
// UI never asks the server to save what the server will reject.
export function isValidEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
}

// The clients page: the whole list of clients plus the form to add one. Server data is read with
// useQuery under the ['clients'] key and changed with useMutation + invalidateQueries — never
// copied into state, never hand-maintained. The only useState here is the two fields the user is
// currently typing into (rule 4).
export function ClientsPage() {
  const queryClient = useQueryClient();
  const clients = useQuery({
    queryKey: ['clients'],
    queryFn: () => getJson<Client[]>('/api/clients'),
  });

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [emailError, setEmailError] = useState(false);

  const create = useMutation({
    mutationFn: () => postJson<Client>('/api/clients', { name, email }),
    onSuccess: () => {
      setName('');
      setEmail('');
      setEmailError(false);
      void queryClient.invalidateQueries({ queryKey: ['clients'] });
    },
  });

  const rows = clients.data ?? [];

  return (
    <section data-testid="clients">
      <form
        data-testid="client-form"
        onSubmit={(e) => {
          e.preventDefault();
          if (!isValidEmail(email)) {
            setEmailError(true);
            return;
          }
          setEmailError(false);
          create.mutate();
        }}
      >
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
        {emailError && (
          <p data-testid="client-form-email-error" role="alert">
            Enter a valid email address.
          </p>
        )}
        <button data-testid="client-form-submit" type="submit">
          Add client
        </button>
      </form>

      {rows.length === 0 ? (
        <p data-testid="clients-empty">No clients yet.</p>
      ) : (
        <table data-testid="clients-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((c) => (
              <tr key={c.id} data-testid={`client-row-${c.id}`}>
                <td data-testid="client-name">{c.name}</td>
                <td data-testid="client-email">{c.email}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
