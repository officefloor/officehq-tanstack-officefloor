import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ClientRow } from '../../slots/defs/clientRow';
import { asNumber, useSearchParam } from '../../url/useSearchParam';
import {
  CLIENT_EDIT_PARAM,
  clientsKey,
  fetchClients,
  isValidEmail,
  updateClient,
} from './queries';

// The "edit this client" action on every client row — its own file (CLAUDE.md rule 3), so the
// clients table never lists what goes at the end of a row. Which row is open outlives the click, so
// it lives in the URL under the shared `editClient` key (rule 4): the Edit button sets it, and each
// row's control reads the same key to decide whether it shows its button or its inline form. Saving
// is a mutation that invalidates ['clients'] (rule 5), so the row re-renders with the new values for
// free. Carries data-testid="client-edit-<id>" and the form anchors (the test contract).
function ClientEdit({ clientId }: { clientId: number }) {
  const [editing, setEditing] = useSearchParam(CLIENT_EDIT_PARAM, asNumber);

  if (editing !== clientId) {
    return (
      <button
        type="button"
        data-testid={`client-edit-${clientId}`}
        onClick={() => setEditing(clientId)}
      >
        Edit
      </button>
    );
  }

  return <ClientEditForm clientId={clientId} onDone={() => setEditing(undefined)} />;
}

// The inline form, mounted only while this row is the one being edited. useState holds only what the
// user is currently typing (CLAUDE.md rule 4), primed once from the already-loaded client row. Email
// is required and must be well-formed: we block the write client-side (and the server enforces the
// same rule) so a bad address can never be saved.
function ClientEditForm({ clientId, onDone }: { clientId: number; onDone: () => void }) {
  const queryClient = useQueryClient();
  const { data: clients } = useQuery({ queryKey: clientsKey, queryFn: fetchClients });
  const client = clients?.find((c) => c.id === clientId);
  const [name, setName] = useState(client?.name ?? '');
  const [email, setEmail] = useState(client?.email ?? '');
  const [emailError, setEmailError] = useState(false);

  const mutation = useMutation({
    mutationFn: () => updateClient(clientId, { name, email }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: clientsKey });
      onDone();
    },
    onError: () => {
      // Server rejected the address (defence in depth) — surface the same error anchor.
      setEmailError(true);
    },
  });

  return (
    <form
      data-testid={`client-edit-form-${clientId}`}
      onSubmit={(event) => {
        event.preventDefault();
        if (!isValidEmail(email)) {
          setEmailError(true);
          return;
        }
        setEmailError(false);
        mutation.mutate();
      }}
    >
      <input
        data-testid="client-edit-form-name"
        placeholder="Name"
        value={name}
        onChange={(event) => setName(event.target.value)}
      />
      <input
        data-testid="client-edit-form-email"
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
        <p data-testid="client-edit-form-email-error" role="alert">
          Enter a valid email address.
        </p>
      )}
      <button data-testid="client-edit-form-submit" type="submit">
        Save
      </button>
    </form>
  );
}

export const contribution = ClientRow.fill({ order: 10, Component: ClientEdit });
