import { useQuery } from '@tanstack/react-query';
import { projectsKey, listProjects, type Project } from './api';
import { ProjectRow } from '../../slots/defs/projectRow';

// The projects list. Reads server data under ['projects'] (never copied into state); the create form
// shares the key, so a successful create refreshes this list with no import between them. Each row
// shows the client's NAME, which the server joins onto the project.
export function ProjectsList() {
  const { data: projects } = useQuery({ queryKey: projectsKey, queryFn: listProjects });

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
        {projects.map((project: Project) => (
          <tr key={project.id} data-testid={`project-row-${project.id}`}>
            <td data-testid="project-name">{project.name}</td>
            <td data-testid="project-client">{project.clientName}</td>
            <td>
              <ProjectRow.Slot projectId={project.id} />
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
