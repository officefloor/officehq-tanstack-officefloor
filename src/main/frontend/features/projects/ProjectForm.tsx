import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  projectsKey,
  createProject,
  clientsKey,
  fetchClientOptions,
} from './queries';

// Add a project for a client. useState holds only what the user is currently entering (CLAUDE.md
// rule 4); the write is a mutation that invalidates ['projects'] so the list refreshes itself.
// The client picker is a <select> whose option values are client ids, read under the shared
// ['clients'] key.
export function ProjectForm() {
  const [name, setName] = useState('');
  const [clientId, setClientId] = useState('');
  const queryClient = useQueryClient();

  const { data: clients } = useQuery({ queryKey: clientsKey, queryFn: fetchClientOptions });

  const mutation = useMutation({
    mutationFn: createProject,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: projectsKey });
      setName('');
      setClientId('');
    },
  });

  return (
    <form
      data-testid="project-form"
      onSubmit={(event) => {
        event.preventDefault();
        if (name.trim() === '' || clientId === '') {
          return;
        }
        mutation.mutate({ name, clientId: Number(clientId) });
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
