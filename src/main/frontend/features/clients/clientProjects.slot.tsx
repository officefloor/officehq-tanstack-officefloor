import { useQuery } from '@tanstack/react-query';
import { ClientDetail } from '../../slots/defs/clientDetail';
import { clientProjectsKey, listClientProjects, type ClientProject } from './api';
import { useSearchParam, asFlag } from '../../url/useSearchParam';

// The projects a client owns — one panel filling the client.detail region. Reads server data under
// ['clients', clientId, 'projects'] (never copied into state); the server scopes to this client, so
// all its projects arrive. Rows reuse the project-row-<id>/project-name anchors — the projects
// listing rendered in the client context.
//
// By default only the ACTIVE projects show (status ACTIVE and not tucked away); the finished and
// hidden (archived) ones drop off until the `clientProjectsAll` URL key is on. The toggle control
// (its own *.slot.tsx) owns that key; this panel reads the same key and filters, so they stay in
// step through the shared search param with nothing passed between them.
function ClientProjects({ clientId }: { clientId: number }) {
  const [showAll] = useSearchParam('clientProjectsAll', asFlag);
  const { data: projects } = useQuery({
    queryKey: clientProjectsKey(clientId),
    queryFn: () => listClientProjects(clientId),
  });

  if (!projects) {
    return null;
  }

  const visible = projects.filter(
    (project: ClientProject) => showAll || (project.status === 'ACTIVE' && !project.archived),
  );

  if (visible.length === 0) {
    return <p data-testid="client-projects-empty">No projects yet.</p>;
  }

  return (
    <table data-testid="client-projects-table">
      <thead>
        <tr>
          <th>Name</th>
        </tr>
      </thead>
      <tbody>
        {visible.map((project: ClientProject) => (
          <tr key={project.id} data-testid={`project-row-${project.id}`}>
            <td data-testid="project-name">{project.name}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export const contribution = ClientDetail.fill({ order: 20, Component: ClientProjects });
