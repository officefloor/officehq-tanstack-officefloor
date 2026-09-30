import { useQuery } from '@tanstack/react-query';
import { clientProjectsKey, fetchClientProjects, type ClientProject } from './queries';

// The projects the user is doing for one client. Queries for itself under ['clients', clientId,
// 'projects'] — never handed its data by a parent (CLAUDE.md rule 5). Reuses the project-row-<id> /
// project-name anchors so the same listing reads the same in the client context.
export function ClientProjectsTable({ clientId }: { clientId: number }) {
  const { data: projects } = useQuery({
    queryKey: clientProjectsKey(clientId),
    queryFn: () => fetchClientProjects(clientId),
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
