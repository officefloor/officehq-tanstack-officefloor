import { useState } from 'react';
import { useClientOptions, useCreateProject, type ProjectStatus } from './projects';

// Add-a-project form. useState holds only what the user is currently entering: the name, the chosen
// client id, the lifecycle status to start the project in, and the short reference code. On submit
// it POSTs {name, clientId, status, code} and invalidates the ['projects'] key. The code must be
// unique across projects (Flyway V29): a blank or already-used code surfaces project-form-code-error
// and never creates a row — a duplicate is rejected by the server (ProjectsPostLogic) and the same
// error is shown, so no duplicate job is added. The client select's option values are client ids and
// the status select's values are the status codes (what the test selects by).
export function ProjectForm() {
  const [name, setName] = useState('');
  const [clientId, setClientId] = useState('');
  const [status, setStatus] = useState<ProjectStatus>('ACTIVE');
  const [code, setCode] = useState('');
  const [codeError, setCodeError] = useState<string | null>(null);
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
        if (!code.trim()) {
          setCodeError('Enter a reference code.');
          return;
        }
        setCodeError(null);
        create.mutate(
          { name, clientId: Number(clientId), status, code: code.trim() },
          {
            onSuccess: () => {
              setName('');
              setClientId('');
              setStatus('ACTIVE');
              setCode('');
            },
            onError: () => setCodeError('That code is already in use.'),
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
      <input
        data-testid="project-form-code"
        placeholder="Code"
        value={code}
        onChange={(event) => {
          setCode(event.target.value);
          if (codeError) {
            setCodeError(null);
          }
        }}
      />
      {codeError && (
        <span data-testid="project-form-code-error" role="alert">
          {codeError}
        </span>
      )}
      <button data-testid="project-form-submit" type="submit">
        Add job
      </button>
    </form>
  );
}
