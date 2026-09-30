import { ProjectsToolbar } from '../../slots/defs/projectsToolbar';
import { useSearchParam, asString } from '../../url/useSearchParam';
import { PROJECTS_STATUS_FILTER_PARAM, PROJECT_STATUSES, type ProjectStatus } from './queries';

// The "filter projects by status" select — its own file filling the projects toolbar region
// (CLAUDE.md rule 3). It owns the `status` URL key (rule 4): the choice outlives a click, so it lives
// in the URL, and the list (ProjectsTable) reads the very same key to decide which rows show. No
// callback, no shared state — just the key. Its options come from the shared PROJECT_STATUSES list
// (rule 5), so the picker and the row's status display agree without importing each other.
const STATUS_LABELS: Record<ProjectStatus, string> = {
  ACTIVE: 'Active',
  ON_HOLD: 'On hold',
  FINISHED: 'Finished',
};

function StatusFilter() {
  const [selected, setSelected] = useSearchParam(PROJECTS_STATUS_FILTER_PARAM, asString);

  return (
    <label>
      Status
      <select
        data-testid="project-status-filter"
        value={selected}
        onChange={(event) => setSelected(event.target.value || undefined)}
      >
        <option value="">All statuses</option>
        {PROJECT_STATUSES.map((status) => (
          <option key={status} value={status}>
            {STATUS_LABELS[status]}
          </option>
        ))}
      </select>
    </label>
  );
}

export const contribution = ProjectsToolbar.fill({ order: 30, Component: StatusFilter });
