import { useQuery } from '@tanstack/react-query';
import {
  projectsKey,
  fetchProjects,
  PROJECTS_SHOW_ARCHIVED_PARAM,
  PROJECTS_TAG_FILTER_PARAM,
  PROJECTS_STATUS_FILTER_PARAM,
} from './queries';
import { ProjectRow } from '../../slots/defs/projectRow';
import { ProjectsToolbar } from '../../slots/defs/projectsToolbar';
import { useSearchParam, asFlag, asNumber, asString } from '../../url/useSearchParam';

// The list. Queries for itself under ['projects'] — never handed its data by a parent (CLAUDE.md
// rule 5). Each row shows the project's name and the client's NAME (from the server-side join).
// Archived projects are tucked away: the toolbar's show-archived toggle owns the `showArchived` URL
// key and the list reads the same key (rule 4), hiding archived rows unless it is on.
export function ProjectsTable() {
  const { data: projects } = useQuery({ queryKey: projectsKey, queryFn: fetchProjects });
  const [showArchived] = useSearchParam(PROJECTS_SHOW_ARCHIVED_PARAM, asFlag);
  const [tagFilter] = useSearchParam(PROJECTS_TAG_FILTER_PARAM, asNumber);
  const [statusFilter] = useSearchParam(PROJECTS_STATUS_FILTER_PARAM, asString);

  if (!projects) {
    return null;
  }

  const visible = projects
    .filter((project) => showArchived || !project.archived)
    .filter((project) => tagFilter === undefined || project.tagIds.includes(tagFilter))
    .filter((project) => statusFilter === '' || project.status === statusFilter);

  if (projects.length === 0) {
    return (
      <>
        <ProjectsToolbar.Slot />
        <p data-testid="projects-empty">No jobs yet.</p>
      </>
    );
  }

  return (
    <>
    <ProjectsToolbar.Slot />
    <table data-testid="projects-table">
      <thead>
        <tr>
          <th>Name</th>
          <th>Code</th>
          <th>Client</th>
          <th />
        </tr>
      </thead>
      <tbody>
        {visible.map((project) => (
          <tr key={project.id} data-testid={`project-row-${project.id}`}>
            <td data-testid="project-name">{project.name}</td>
            <td data-testid="project-code">{project.code}</td>
            <td data-testid="project-client">{project.clientName}</td>
            <td>
              <ProjectRow.Slot projectId={project.id} />
            </td>
          </tr>
        ))}
      </tbody>
    </table>
    </>
  );
}
