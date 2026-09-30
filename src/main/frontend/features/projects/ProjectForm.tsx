import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  projectsKey,
  createProject,
  clientsKey,
  fetchClientOptions,
  PROJECT_STATUSES,
  type ProjectStatus,
} from './queries';

// Add a project for a client. useState holds only what the user is currently entering (CLAUDE.md
// rule 4); the write is a mutation that invalidates ['projects'] so the list refreshes itself.
// The client picker is a <select> whose option values are client ids, read under the shared
// ['clients'] key.
export function ProjectForm() {
  const [name, setName] = useState('');
  const [clientId, setClientId] = useState('');
  const [status, setStatus] = useState<ProjectStatus>('ACTIVE');
  const [code, setCode] = useState('');
  const [codeError, setCodeError] = useState(false);
  const queryClient = useQueryClient();

  const { data: clients } = useQuery({ queryKey: clientsKey, queryFn: fetchClientOptions });

  const mutation = useMutation({
    mutationFn: createProject,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: projectsKey });
      setName('');
      setClientId('');
      setStatus('ACTIVE');
      setCode('');
    },
    onError: () => {
      // Server rejected the code (missing or already in use) — surface the error anchor.
      setCodeError(true);
    },
  });

  return (
    <form
      data-testid="project-form"
      onSubmit={(event) => {
        event.preventDefault();
        if (name.trim() === '' || clientId === '' || code.trim() === '') {
          return;
        }
        setCodeError(false);
        mutation.mutate({ name, clientId: Number(clientId), status, code: code.trim() });
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
        {PROJECT_STATUSES.map((option) => (
          <option key={option} value={option}>
            {option}
          </option>
        ))}
      </select>
      <input
        data-testid="project-form-code"
        placeholder="Code"
        value={code}
        onChange={(event) => {
          setCode(event.target.value);
          if (codeError) {
            setCodeError(false);
          }
        }}
      />
      {codeError && (
        <p data-testid="project-form-code-error" role="alert">
          Enter a unique reference code.
        </p>
      )}
      <button data-testid="project-form-submit" type="submit">
        Add job
      </button>
    </form>
  );
}
