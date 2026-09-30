import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import type { Client } from '../clients/ClientsPage';

// A project as the server returns it: it carries the client's NAME so the list shows the name, not
// the id. project.clientId is the id the form's select submits.
export type Project = { id: number; name: string; clientId: number; clientName: string };

// The projects page: the whole list of projects plus the form to add one and pick its client.
// Server data is read with useQuery (['projects'] for the list, ['clients'] — the SAME key the
// clients feature owns — for the select options) and changed with useMutation + invalidateQueries.
// The only useState here is the fields the user is currently filling in (rule 4).
export function ProjectsPage() {
  const queryClient = useQueryClient();
  const projects = useQuery({
    queryKey: ['projects'],
    queryFn: () => getJson<Project[]>('/api/projects'),
  });
  const clients = useQuery({
    queryKey: ['clients'],
    queryFn: () => getJson<Client[]>('/api/clients'),
  });

  const [name, setName] = useState('');
  const [clientId, setClientId] = useState('');

  const create = useMutation({
    mutationFn: () => postJson<Project>('/api/projects', { name, clientId: Number(clientId) }),
    onSuccess: () => {
      setName('');
      setClientId('');
      void queryClient.invalidateQueries({ queryKey: ['projects'] });
    },
  });

  const rows = projects.data ?? [];
  const clientOptions = clients.data ?? [];

  return (
    <section data-testid="projects">
      <form
        data-testid="project-form"
        onSubmit={(e) => {
          e.preventDefault();
          if (!name.trim() || !clientId) {
            return;
          }
          create.mutate();
        }}
      >
        <input
          data-testid="project-form-name"
          placeholder="Project name"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <select
          data-testid="project-form-client"
          value={clientId}
          onChange={(e) => setClientId(e.target.value)}
        >
          <option value="">Select a client</option>
          {clientOptions.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        <button data-testid="project-form-submit" type="submit">
          Add project
        </button>
      </form>

      {rows.length === 0 ? (
        <p data-testid="projects-empty">No projects yet.</p>
      ) : (
        <table data-testid="projects-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Client</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((p) => (
              <tr key={p.id} data-testid={`project-row-${p.id}`}>
                <td data-testid="project-name">{p.name}</td>
                <td data-testid="project-client">{p.clientName}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
