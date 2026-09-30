import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asFlag, useSearchParam } from '../../url/useSearchParam';
import type { Project } from './ProjectsPage';

// A client's projects: the projects listing scoped to one client, rendered in the client-detail
// context. Server data is read with useQuery under the SAME ['projects'] key the projects page owns
// (so a project write that invalidates ['projects'] refreshes this too) and filtered to the client
// it is handed — never copied into state, never hand-maintained (rule 5). Reuses the
// project-row-<id>/project-name anchors inside its own client-projects-table.
export function ClientProjectsPanel({ clientId }: { clientId: number }) {
  const projects = useQuery({
    queryKey: ['projects'],
    queryFn: () => getJson<Project[]>('/api/projects'),
  });

  // Whether the finished and hidden (archived) projects are revealed lives in the URL, owned by
  // features/projects/clientProjectsShowAll.slot.tsx. This panel reads the same key: by default it
  // shows only the client's live work (ACTIVE and not archived); flip the key and every project
  // for the client shows. Nothing is passed between the toggle and the panel (rule 4).
  const [showAll] = useSearchParam('clientProjectsShowAll', asFlag);

  const rows = (projects.data ?? []).filter(
    (p) => p.clientId === clientId && (showAll || (p.status === 'ACTIVE' && !p.archived)),
  );

  return (
    <section data-testid="client-projects">
      {rows.length === 0 ? (
        <p data-testid="client-projects-empty">No projects yet.</p>
      ) : (
        <table data-testid="client-projects-table">
          <thead>
            <tr>
              <th>Name</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((p) => (
              <tr key={p.id} data-testid={`project-row-${p.id}`}>
                <td data-testid="project-name">{p.name}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
