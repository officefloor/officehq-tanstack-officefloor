import { ProjectsToolbar } from '../../slots/defs/projectsToolbar';
import { useSearchParam, asString } from '../../url/useSearchParam';
import { projectStatuses } from './api';

// A control over the projects list — one new *.slot.tsx filling the projects.toolbar region. It owns
// the `projectStatus` URL key; the list reads the same key and keeps only the projects at that
// lifecycle status. Nothing is passed between them: they stay in step through the shared search
// param. The empty option clears the key, so the list falls back to showing every project. The
// options are the known statuses (ACTIVE / ON_HOLD / FINISHED), shared by the api module.
function ProjectStatusFilter() {
  const [status, setStatus] = useSearchParam('projectStatus', asString);
  return (
    <select
      data-testid="project-status-filter"
      value={status}
      onChange={(e) => setStatus(e.target.value)}
    >
      <option value="">All statuses</option>
      {projectStatuses.map((s) => (
        <option key={s} value={s}>
          {s}
        </option>
      ))}
    </select>
  );
}

export const contribution = ProjectsToolbar.fill({ order: 30, Component: ProjectStatusFilter });
