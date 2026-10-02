import { useState } from 'react';
import { ClientRow } from '../../slots/defs/clientRow';
import { asNumber, useSearchParam } from '../../url/useSearchParam';
import { isValidEmail } from './email';
import type { Client } from './clients';
import { useUpdateClient } from './clients';

// "Correct this client" — a self-contained row action filling the client-row region. Which row is
// open for editing outlives the click, so it lives in the URL (`editClient` holds the client id),
// not in a parent: each row reads the same key and only the matching row swaps its Edit button for
// an inline form. useState holds only what is being typed into the form (seeded from the current
// values); on submit a well-formed email POSTs through the shared mutation, which invalidates
// ['clients'] so the list re-reads and the corrected name/email show. A blank or malformed email
// surfaces client-edit-form-email-error and never saves; an email already in use by another client
// is rejected by the server and shows the same error. No feature is edited to add this — one file.
function EditClient({ client }: { client: Client }) {
  const [editingId, setEditingId] = useSearchParam('editClient', asNumber);

  if (editingId === client.id) {
    return <EditForm client={client} onDone={() => setEditingId(undefined)} />;
  }

  return (
    <button
      type="button"
      data-testid={`client-edit-${client.id}`}
      onClick={() => setEditingId(client.id)}
    >
      Edit
    </button>
  );
}

function EditForm({ client, onDone }: { client: Client; onDone: () => void }) {
  const [name, setName] = useState(client.name);
  const [email, setEmail] = useState(client.email);
  const [emailError, setEmailError] = useState<string | null>(null);
  const update = useUpdateClient();

  return (
    <form
      data-testid="client-edit-form"
      onSubmit={(event) => {
        event.preventDefault();
        if (!isValidEmail(email)) {
          setEmailError('Enter a valid email address.');
          return;
        }
        setEmailError(null);
        update.mutate(
          { id: client.id, name, email },
          {
            onSuccess: () => onDone(),
            onError: () => setEmailError('That email is already in use.'),
          },
        );
      }}
    >
      <input
        data-testid="client-edit-form-name"
        value={name}
        onChange={(event) => setName(event.target.value)}
      />
      <input
        data-testid="client-edit-form-email"
        value={email}
        onChange={(event) => {
          setEmail(event.target.value);
          if (emailError) {
            setEmailError(null);
          }
        }}
      />
      {emailError && (
        <span data-testid="client-edit-form-email-error" role="alert">
          {emailError}
        </span>
      )}
      <button data-testid="client-edit-form-submit" type="submit" disabled={update.isPending}>
        Save
      </button>
      <button type="button" data-testid={`client-edit-cancel-${client.id}`} onClick={onDone}>
        Cancel
      </button>
    </form>
  );
}

export const contribution = ClientRow.fill({ order: 10, Component: EditClient });
