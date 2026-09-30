import { useQuery } from '@tanstack/react-query';
import {
  clientProjectsKey,
  fetchClientProjects,
  isActiveProject,
  CLIENT_PROJECTS_SHOW_ALL_PARAM,
  type ClientProject,
} from './queries';
import { useSearchParam, asFlag } from '../../url/useSearchParam';

// The projects the user is doing for one client. Queries for itself under ['clients', clientId,
// 'projects'] — never handed its data by a parent (CLAUDE.md rule 5). Reuses the project-row-<id> /
// project-name anchors so the same listing reads the same in the client context.
//
// By default only the ACTIVE, non-archived projects show. The show-all toggle (its own slot file)
// owns the `clientProjectsAll` URL key and this table reads the same key (CLAUDE.md rule 4): when it
// is on, the finished and hidden (archived) projects are revealed too.
export function ClientProjectsTable({ clientId }: { clientId: number }) {
  const { data: projects } = useQuery({
    queryKey: clientProjectsKey(clientId),
    queryFn: () => fetchClientProjects(clientId),
  });
  const [showAll] = useSearchParam(CLIENT_PROJECTS_SHOW_ALL_PARAM, asFlag);

  if (!projects) {
    return null;
  }

  if (projects.length === 0) {
    return <p data-testid="client-projects-empty">No jobs yet.</p>;
  }

  const visible = projects.filter((project) => showAll || isActiveProject(project));

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
