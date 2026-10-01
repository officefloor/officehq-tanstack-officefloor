import { useQuery } from '@tanstack/react-query';
import { projectsKey, listProjects, type Project } from './api';
import { ProjectRow } from '../../slots/defs/projectRow';
import { ProjectsToolbar } from '../../slots/defs/projectsToolbar';
import { useSearchParam, asFlag } from '../../url/useSearchParam';

// The projects list. Reads server data under ['projects'] (never copied into state); the create form
// shares the key, so a successful create refreshes this list with no import between them. Each row
// shows the client's NAME, which the server joins onto the project.
//
// Archived projects are tucked away: they drop off the list unless the `showArchived` URL key is on.
// The toggle control (its own *.slot.tsx) owns that key; this list reads the same key and filters,
// so they stay in step through the shared search param with nothing passed between them.
export function ProjectsList() {
  const [showArchived] = useSearchParam('showArchived', asFlag);
  const { data: projects } = useQuery({ queryKey: projectsKey, queryFn: listProjects });

  if (!projects) {
    return null;
  }

  const visible = projects.filter((project: Project) => showArchived || !project.archived);

  return (
    <>
      <ProjectsToolbar.Slot />
      {visible.length === 0 ? (
        <p data-testid="projects-empty">No projects yet.</p>
      ) : (
        <ProjectsTable projects={visible} />
      )}
    </>
  );
}

function ProjectsTable({ projects }: { projects: Project[] }) {
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
