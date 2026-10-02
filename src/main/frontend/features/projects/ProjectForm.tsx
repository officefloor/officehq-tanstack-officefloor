import { useState } from 'react';
import { useClientOptions, useCreateProject, type ProjectStatus } from './projects';

// Add-a-project form. useState holds only what the user is currently entering: the name, the chosen
// client id, and the lifecycle status to start the project in. On submit it POSTs
// {name, clientId, status} and invalidates the ['projects'] key. The client select's option values
// are client ids and the status select's values are the status codes (what the test selects by).
export function ProjectForm() {
  const [name, setName] = useState('');
  const [clientId, setClientId] = useState('');
  const [status, setStatus] = useState<ProjectStatus>('ACTIVE');
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
          { name, clientId: Number(clientId), status },
          {
            onSuccess: () => {
              setName('');
              setClientId('');
              setStatus('ACTIVE');
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
      <select
        data-testid="project-form-status"
        value={status}
        onChange={(event) => setStatus(event.target.value as ProjectStatus)}
      >
        <option value="ACTIVE">Active</option>
        <option value="ON_HOLD">On hold</option>
        <option value="FINISHED">Finished</option>
      </select>
      <button data-testid="project-form-submit" type="submit">
        Add job
      </button>
    </form>
  );
}
