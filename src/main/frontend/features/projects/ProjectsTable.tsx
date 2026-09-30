import { useQuery } from '@tanstack/react-query';
import { projectsKey, fetchProjects } from './queries';
import { ProjectRow } from '../../slots/defs/projectRow';

// The list. Queries for itself under ['projects'] — never handed its data by a parent (CLAUDE.md
// rule 5). Each row shows the project's name and the client's NAME (from the server-side join).
export function ProjectsTable() {
  const { data: projects } = useQuery({ queryKey: projectsKey, queryFn: fetchProjects });

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
          <th />
        </tr>
      </thead>
      <tbody>
        {projects.map((project) => (
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
