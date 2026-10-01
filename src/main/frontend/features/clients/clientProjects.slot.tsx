import { useQuery } from '@tanstack/react-query';
import { ClientDetail } from '../../slots/defs/clientDetail';
import { clientProjectsKey, listClientProjects, type ClientProject } from './api';

// The projects a client owns — one panel filling the client.detail region. Reads server data under
// ['clients', clientId, 'projects'] (never copied into state); the server scopes to this client, so
// only its projects arrive. Rows reuse the project-row-<id>/project-name anchors — the projects
// listing rendered in the client context.
function ClientProjects({ clientId }: { clientId: number }) {
  const { data: projects } = useQuery({
    queryKey: clientProjectsKey(clientId),
    queryFn: () => listClientProjects(clientId),
  });

  if (!projects) {
    return null;
  }

  if (projects.length === 0) {
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
        {projects.map((project: ClientProject) => (
          <tr key={project.id} data-testid={`project-row-${project.id}`}>
            <td data-testid="project-name">{project.name}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export const contribution = ClientDetail.fill({ order: 20, Component: ClientProjects });
