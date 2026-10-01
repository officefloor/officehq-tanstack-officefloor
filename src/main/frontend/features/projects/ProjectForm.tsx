import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  clientsKey,
  createProject,
  listClientOptions,
  projectsKey,
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

  const mutation = useMutation({
    mutationFn: createProject,
    onSuccess: () => {
      setName('');
      setClientId('');
      void queryClient.invalidateQueries({ queryKey: projectsKey });
    },
  });

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (name.trim() === '' || clientId === '') {
      return;
    }
    mutation.mutate({ name: name.trim(), clientId: Number(clientId) });
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
      <button data-testid="project-form-submit" type="submit">
        Add project
      </button>
    </form>
  );
}
