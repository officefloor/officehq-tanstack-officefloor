import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { ClientsToolbar } from '../../slots/defs/clientsToolbar';
import { ClientRowActions } from '../../slots/defs/clientRowActions';

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
  // The name filter lives in the URL (owned by features/clients/search.slot.tsx). The list reads
  // the same key and asks the server for the narrowed list, so it stays a single source of truth —
  // no server data copied into state. The key still starts with 'clients', so writes that
  // invalidateQueries({ queryKey: ['clients'] }) still refresh it.
  const [q] = useSearchParam('clientSearch', asString);
  // The sort lives in the URL too (owned by features/clients/sort.slot.tsx). The list reads the
  // same key and asks the server for the ordered list, so it stays a single source of truth — no
  // server data copied into state or re-sorted by hand. The key still starts with 'clients', so
  // writes that invalidateQueries({ queryKey: ['clients'] }) still refresh it.
  const [sort] = useSearchParam('clientSort', asString);
  const clients = useQuery({
    queryKey: ['clients', q, sort],
    queryFn: () => {
      const params = new URLSearchParams();
      if (q) params.set('q', q);
      if (sort) params.set('sort', sort);
      const qs = params.toString();
      return getJson<Client[]>(qs ? `/api/clients?${qs}` : '/api/clients');
    },
  });

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  // The email error message, or null when the field is fine. It covers both a locally-caught bad
  // format and a server rejection (a duplicate email — the server is the source of truth for
  // uniqueness, since another session may have taken the address).
  const [emailError, setEmailError] = useState<string | null>(null);

  const create = useMutation({
    mutationFn: () => postJson<Client>('/api/clients', { name, email }),
    onSuccess: () => {
      setName('');
      setEmail('');
      setEmailError(null);
      void queryClient.invalidateQueries({ queryKey: ['clients'] });
    },
    onError: () => {
      setEmailError('That email is already in use.');
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
            setEmailError('Enter a valid email address.');
            return;
          }
          setEmailError(null);
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
            {emailError}
          </p>
        )}
        <button data-testid="client-form-submit" type="submit">
          Add client
        </button>
      </form>

      <div data-testid="clients-toolbar">
        <ClientsToolbar.Slot />
      </div>

      {rows.length === 0 ? (
        <p data-testid="clients-empty">No clients yet.</p>
      ) : (
        <table data-testid="clients-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {rows.map((c) => (
              <tr key={c.id} data-testid={`client-row-${c.id}`}>
                <td data-testid="client-name">{c.name}</td>
                <td data-testid="client-email">{c.email}</td>
                <td>
                  <ClientRowActions.Slot clientId={c.id} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
