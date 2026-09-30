import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { asFlag, asNumber, asString, useSearchParam } from '../../url/useSearchParam';
import type { Client } from '../clients/ClientsPage';
import type { ProjectTag } from './ProjectTagsPanel';
import { ProjectRowActions } from '../../slots/defs/projectRowActions';
import { ProjectsToolbar } from '../../slots/defs/projectsToolbar';

// A project as the server returns it: it carries the client's NAME so the list shows the name, not
// the id. project.clientId is the id the form's select submits. `archived` projects are kept but
// hidden from the default list.
// A project's state: ACTIVE while worked on, ON_HOLD when paused, FINISHED when done. Stored on the
// row and pickable when creating a project (V21__project_status.sql).
export const PROJECT_STATUSES = ['ACTIVE', 'ON_HOLD', 'FINISHED'] as const;
export type ProjectStatus = (typeof PROJECT_STATUSES)[number];

export type Project = {
  id: number;
  name: string;
  clientId: number;
  clientName: string;
  archived: boolean;
  status: ProjectStatus;
};

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

  // Whether archived projects are revealed lives in the URL, owned by
  // features/projects/showArchived.slot.tsx. The page reads the same key: by default archived
  // projects drop off the list; the toggle brings them back — no server data copied into state.
  const [showArchived] = useSearchParam('showArchived', asFlag);

  // Which tag the list is filtered to lives in the URL, owned by features/projects/tagFilter.slot.tsx.
  // The page reads the same key and narrows its rows to projects carrying that tag; the pairings come
  // from useQuery under ['projectTags'] — the SAME key the tags panel owns (rule 5) — so nothing is
  // passed between the control and the list. Undefined means no filter: every project shows.
  const [tagFilter] = useSearchParam('projectTag', asNumber);

  // Which status the list is filtered to lives in the URL, owned by
  // features/projects/statusFilter.slot.tsx. The page reads the same key and narrows its rows to
  // projects at that status; empty means no filter, so every project shows.
  const [statusFilter] = useSearchParam('projectStatus', asString);
  const projectTags = useQuery({
    queryKey: ['projectTags'],
    queryFn: () => getJson<ProjectTag[]>('/api/project-tags'),
  });

  const [name, setName] = useState('');
  const [clientId, setClientId] = useState('');
  const [status, setStatus] = useState<ProjectStatus>('ACTIVE');

  const create = useMutation({
    mutationFn: () =>
      postJson<Project>('/api/projects', { name, clientId: Number(clientId), status }),
    onSuccess: () => {
      setName('');
      setClientId('');
      setStatus('ACTIVE');
      void queryClient.invalidateQueries({ queryKey: ['projects'] });
    },
  });

  const taggedIds = new Set(
    (projectTags.data ?? [])
      .filter((pt) => pt.tagId === tagFilter)
      .map((pt) => pt.projectId),
  );
  const rows = (projects.data ?? [])
    .filter((p) => showArchived || !p.archived)
    .filter((p) => tagFilter === undefined || taggedIds.has(p.id))
    .filter((p) => statusFilter === '' || p.status === statusFilter);
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
        <select
          data-testid="project-form-status"
          value={status}
          onChange={(e) => setStatus(e.target.value as ProjectStatus)}
        >
          {PROJECT_STATUSES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
        <button data-testid="project-form-submit" type="submit">
          Add project
        </button>
      </form>

      <div data-testid="projects-toolbar">
        <ProjectsToolbar.Slot />
      </div>

      {rows.length === 0 ? (
        <p data-testid="projects-empty">No projects yet.</p>
      ) : (
        <table data-testid="projects-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Client</th>
              <th>Status</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {rows.map((p) => (
              <tr key={p.id} data-testid={`project-row-${p.id}`}>
                <td data-testid="project-name">{p.name}</td>
                <td data-testid="project-client">{p.clientName}</td>
                <td data-testid="project-status">{p.status}</td>
                <td>
                  <ProjectRowActions.Slot projectId={p.id} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
