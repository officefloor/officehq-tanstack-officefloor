import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ClientRow } from '../../slots/defs/clientRow';
import { useSearchParam, asNumber } from '../../url/useSearchParam';
import { clientsKey, listClients, updateClient, type Client } from './api';

// Correct a client's name or email from the list — one new file filling the per-client-row action
// slot. Which row is open for editing outlives the click, so it lives in the URL: this control owns
// the `edit` key (holding the client id being edited), never a useState and never a flag passed
// between rows. The Edit button sets the key to its id; the row whose id matches swaps its button
// for an inline form. Saving is a useMutation that invalidates the shared ['clients'] key so the
// list refetches with the correction — we never hand-maintain the list. Its anchors are
// client-edit-<id> (the button) and client-edit-form-{name,email,submit} (the test contract).

// A proper email, mirrored from ClientForm.tsx (the server check in UpdateClient.java and the DB
// CHECK constraint are the last lines of defence).
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function EditClientForm({ client, onClose }: { client: Client; onClose: () => void }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState(client.name);
  const [email, setEmail] = useState(client.email);
  const [emailError, setEmailError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () => updateClient({ id: client.id, name, email: email.trim() }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: clientsKey });
      onClose();
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
    mutation.mutate();
  };

  return (
    <form onSubmit={submit}>
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
        onChange={(e) => {
          setEmail(e.target.value);
          if (emailError) {
            setEmailError(null);
          }
        }}
      />
      {emailError && (
        <p data-testid="client-edit-form-email-error" role="alert">
          {emailError}
        </p>
      )}
      <button data-testid="client-edit-form-submit" type="submit" disabled={mutation.isPending}>
        Save
      </button>
      <button type="button" onClick={onClose}>
        Cancel
      </button>
    </form>
  );
}

function EditClient({ clientId }: { clientId: number }) {
  const [editing, setEditing] = useSearchParam('edit', asNumber);
  const { data: clients } = useQuery({ queryKey: clientsKey, queryFn: listClients });
  const client = clients?.find((c) => c.id === clientId);

  if (editing === clientId && client) {
    return <EditClientForm client={client} onClose={() => setEditing(undefined)} />;
  }

  return (
    <button
      data-testid={`client-edit-${clientId}`}
      type="button"
      onClick={() => setEditing(clientId)}
    >
      Edit
    </button>
  );
}

export const contribution = ClientRow.fill({ order: 10, Component: EditClient });
