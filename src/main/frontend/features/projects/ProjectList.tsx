import { Link } from '@tanstack/react-router';
import { ProjectRow } from '../../slots/defs/projectRow';
import { ProjectsToolbar } from '../../slots/defs/projectsToolbar';
import { useSearchParam, asFlag } from '../../url/useSearchParam';
import { useProjects } from './projects';

// The projects list. Reads server data straight from its query key and shows the client's NAME
// (served alongside each project), not the id. Archived projects are tucked away: they drop off the
// list unless the shared `showArchived` URL key (set by a toolbar control) reveals them.
export function ProjectList() {
  const { data: projects } = useProjects();
  const [showArchived] = useSearchParam('showArchived', asFlag);

  if (!projects) {
    return null;
  }

  if (projects.length === 0) {
    return <p data-testid="projects-empty">No projects yet.</p>;
  }

  const shown = showArchived ? projects : projects.filter((project) => !project.archived);

  return (
    <>
    <ProjectsToolbar.Slot />
    <table data-testid="projects-table">
      <thead>
        <tr>
          <th>Name</th>
          <th>Client</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {shown.map((project) => (
          <tr key={project.id} data-testid={`project-row-${project.id}`}>
            <td data-testid="project-name">{project.name}</td>
            <td data-testid="project-client">{project.clientName}</td>
            <td>
              <Link
                to="/projects/$projectId"
                params={{ projectId: String(project.id) }}
                data-testid={`project-open-${project.id}`}
              >
                Open
              </Link>
            </td>
            <td>
              <ProjectRow.Slot project={project} />
            </td>
          </tr>
        ))}
      </tbody>
    </table>
    </>
  );
}
