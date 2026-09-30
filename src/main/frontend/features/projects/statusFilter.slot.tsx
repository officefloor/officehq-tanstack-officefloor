import { ProjectsToolbar } from '../../slots/defs/projectsToolbar';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { PROJECT_STATUSES } from './ProjectsPage';

// The by-status project filter — its own file, filling the projects toolbar slot (order 30, after
// the show-archived toggle at 10 and the tag filter at 20). It OWNS the `projectStatus` URL key
// (rule 4: a filter outlives a click, so it lives in the URL, not useState). The projects page reads
// the same key and narrows its rows to projects at that status; nothing is passed between them.
// Carries data-testid="project-status-filter" (the test contract); the empty option clears the key,
// showing every project again.
function StatusFilter() {
  const [status, setStatus] = useSearchParam('projectStatus', asString);
  return (
    <select
      data-testid="project-status-filter"
      value={status}
      onChange={(e) => setStatus(e.target.value || undefined)}
    >
      <option value="">All statuses</option>
      {PROJECT_STATUSES.map((s) => (
        <option key={s} value={s}>
          {s}
        </option>
      ))}
    </select>
  );
}

export const contribution = ProjectsToolbar.fill({ order: 30, Component: StatusFilter });
