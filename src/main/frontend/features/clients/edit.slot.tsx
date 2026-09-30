import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { ClientRowActions } from '../../slots/defs/clientRowActions';
import { isValidEmail, type Client } from './ClientsPage';

// Correcting a client's name or email — its own file, filling the client-row-actions slot. The Edit
// button OWNS the `editClient` URL key (rule 4: which row is open outlives a click, so it lives in
// the URL, not useState); only one row is ever open, so the form carries unsuffixed testids. The
// form queries the client for itself (rule 5) and writes with a useMutation that POSTs to
// /api/clients/update, then invalidates ['clients'] so the list (and search, which shares the key)
// refetches and shows the corrected row. Carries data-testid="client-edit-<id>" (the test contract).
function ClientEdit({ clientId }: { clientId: number }) {
  const [editing, setEditing] = useSearchParam('editClient', asString);
  const open = editing === String(clientId);
  return (
    <>
      <button
        type="button"
        data-testid={`client-edit-${clientId}`}
        aria-pressed={open}
        onClick={() => setEditing(open ? undefined : String(clientId))}
      >
        {open ? 'Cancel' : 'Edit'}
      </button>
      {open && <ClientEditForm clientId={clientId} />}
    </>
  );
}

// The edit form. It reads the client's current values for itself and mounts EditFields keyed by the
// client id, so the fields start pre-filled with the values the user is about to correct.
function ClientEditForm({ clientId }: { clientId: number }) {
  const clients = useQuery({
    queryKey: ['clients'],
    queryFn: () => getJson<Client[]>('/api/clients'),
  });
  const client = clients.data?.find((c) => c.id === clientId);
  if (!client) {
    return null;
  }
  return <EditFields key={client.id} client={client} />;
}

function EditFields({ client }: { client: Client }) {
  const queryClient = useQueryClient();
  const [, setEditing] = useSearchParam('editClient', asString);
  // useState only for the fields the user is currently editing (rule 4), seeded once at mount from
  // the current values so a single field can be corrected without retyping the other.
  const [name, setName] = useState(client.name);
  const [email, setEmail] = useState(client.email);
  const [emailError, setEmailError] = useState<string | null>(null);

  const update = useMutation({
    mutationFn: () => postJson<Client>('/api/clients/update', { id: client.id, name, email }),
    onSuccess: () => {
      setEditing(undefined);
      void queryClient.invalidateQueries({ queryKey: ['clients'] });
    },
    onError: () => {
      setEmailError('That email is already in use.');
    },
  });

  return (
    <form
      data-testid="client-edit-form"
      onSubmit={(e) => {
        e.preventDefault();
        if (!isValidEmail(email)) {
          setEmailError('Enter a valid email address.');
          return;
        }
        setEmailError(null);
        update.mutate();
      }}
    >
      <input
        data-testid="client-edit-form-name"
        placeholder="Name"
        value={name}
        onChange={(e) => setName(e.target.value)}
      />
      <input
        data-testid="client-edit-form-email"
        placeholder="Email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />
      {emailError && (
        <p data-testid="client-edit-form-email-error" role="alert">
          {emailError}
        </p>
      )}
      <button data-testid="client-edit-form-submit" type="submit" disabled={update.isPending}>
        Save
      </button>
    </form>
  );
}

export const contribution = ClientRowActions.fill({ order: 5, Component: ClientEdit });
