import { Link } from '@tanstack/react-router';
import { ProjectRow } from '../../slots/defs/projectRow';
import { useProjects } from './projects';

// The projects list. Reads server data straight from its query key and shows the client's NAME
// (served alongside each project), not the id.
export function ProjectList() {
  const { data: projects } = useProjects();

  if (!projects) {
    return null;
  }

  if (projects.length === 0) {
    return <p data-testid="projects-empty">No projects yet.</p>;
  }

  return (
    <table data-testid="projects-table">
      <thead>
        <tr>
          <th>Name</th>
          <th>Client</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {projects.map((project) => (
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
  );
}
