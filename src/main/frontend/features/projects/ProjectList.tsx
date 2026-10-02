import { Link } from '@tanstack/react-router';
import { ProjectRow } from '../../slots/defs/projectRow';
import { ProjectsToolbar } from '../../slots/defs/projectsToolbar';
import { useSearchParam, asFlag, asString } from '../../url/useSearchParam';
import { useProjects } from './projects';

// The projects list. Reads server data straight from its query key and shows the client's NAME
// (served alongside each project), not the id. Archived projects are tucked away: they drop off the
// list unless the shared `showArchived` URL key (set by a toolbar control) reveals them.
export function ProjectList() {
  const { data: projects } = useProjects();
  const [showArchived] = useSearchParam('showArchived', asFlag);
  // Shared with the tag-filter toolbar control via the `projectTag` key (no import). When set, keep
  // only projects carrying that tag; empty shows every tag.
  const [tagFilter] = useSearchParam('projectTag', asString);
  // Shared with the status-filter toolbar control via the `projectStatus` key (no import). When set,
  // keep only projects at that lifecycle status; empty shows every status.
  const [statusFilter] = useSearchParam('projectStatus', asString);

  if (!projects) {
    return null;
  }

  if (projects.length === 0) {
    return <p data-testid="projects-empty">No jobs yet.</p>;
  }

  const tagId = tagFilter === '' ? undefined : Number(tagFilter);
  const shown = projects
    .filter((project) => showArchived || !project.archived)
    .filter((project) => tagId === undefined || project.tagIds.includes(tagId))
    .filter((project) => statusFilter === '' || project.status === statusFilter);

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
