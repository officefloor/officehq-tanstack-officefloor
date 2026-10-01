import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  clientsKey,
  createProject,
  listClientOptions,
  projectsKey,
  projectStatuses,
  type ProjectStatus,
} from './api';

// Add a project for a client. The fields the user is typing live in useState (uncommitted input);
// the saved data lives on the server. The client select is populated from the ['clients'] query —
// each option's value is the client id — so the user picks which client the project is for. On
// success we invalidate ['projects'] so the list refetches — we never hand-maintain the list.
export function ProjectForm() {
  const queryClient = useQueryClient();
  const { data: clients } = useQuery({ queryKey: clientsKey, queryFn: listClientOptions });
  const [name, setName] = useState('');
  const [clientId, setClientId] = useState('');
  // The lifecycle the new project starts in — uncommitted input like name/client, so it lives in
  // useState until submit. Defaults to ACTIVE, the state a fresh project gets.
  const [status, setStatus] = useState<ProjectStatus>('ACTIVE');
  // The reference code typed for the new project — uncommitted input, so it lives in useState until
  // submit. Uniqueness can only be known server-side (another project may already hold it), so the
  // server rejects a duplicate and we surface that on its own error anchor.
  const [code, setCode] = useState('');
  const [codeError, setCodeError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: createProject,
    onSuccess: () => {
      setName('');
      setClientId('');
      setStatus('ACTIVE');
      setCode('');
      setCodeError(null);
      void queryClient.invalidateQueries({ queryKey: projectsKey });
    },
    onError: () => {
      setCodeError('That code is already in use.');
    },
  });

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (name.trim() === '' || clientId === '' || code.trim() === '') {
      return;
    }
    setCodeError(null);
    mutation.mutate({ name: name.trim(), clientId: Number(clientId), status, code: code.trim() });
  };

  return (
    <form onSubmit={submit}>
      <input
        data-testid="project-form-name"
        placeholder="Name"
        value={name}
        onChange={(e) => setName(e.target.value)}
      />
      <select
        data-testid="project-form-client"
        value={clientId}
        onChange={(e) => setClientId(e.target.value)}
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
        onChange={(e) => setStatus(e.target.value as ProjectStatus)}
      >
        {projectStatuses.map((s) => (
          <option key={s} value={s}>
            {s}
          </option>
        ))}
      </select>
      <input
        data-testid="project-form-code"
        placeholder="Code"
        value={code}
        onChange={(e) => {
          setCode(e.target.value);
          if (codeError) {
            setCodeError(null);
          }
        }}
      />
      {codeError && (
        <p data-testid="project-form-code-error" role="alert">
          {codeError}
        </p>
      )}
      <button data-testid="project-form-submit" type="submit">
        Add job
      </button>
    </form>
  );
}
