import { useState } from 'react';
import { useClientOptions, useCreateProject } from './projects';

// Add-a-project form. useState holds only what the user is currently entering: the name and the
// chosen client id. On submit it POSTs {name, clientId} and invalidates the ['projects'] key. The
// client select's option values are client ids (what the test selects by).
export function ProjectForm() {
  const [name, setName] = useState('');
  const [clientId, setClientId] = useState('');
  const { data: clients } = useClientOptions();
  const create = useCreateProject();

  return (
    <form
      data-testid="project-form"
      onSubmit={(event) => {
        event.preventDefault();
        if (!name.trim() || !clientId) {
          return;
        }
        create.mutate(
          { name, clientId: Number(clientId) },
          {
            onSuccess: () => {
              setName('');
              setClientId('');
            },
          },
        );
      }}
    >
      <input
        data-testid="project-form-name"
        placeholder="Name"
        value={name}
        onChange={(event) => setName(event.target.value)}
      />
      <select
        data-testid="project-form-client"
        value={clientId}
        onChange={(event) => setClientId(event.target.value)}
      >
        <option value="">Select a client</option>
        {(clients ?? []).map((client) => (
          <option key={client.id} value={client.id}>
            {client.name}
          </option>
        ))}
      </select>
      <button data-testid="project-form-submit" type="submit">
        Add project
      </button>
    </form>
  );
}
