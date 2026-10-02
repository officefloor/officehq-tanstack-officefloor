import { ProjectsToolbar } from '../../slots/defs/projectsToolbar';
import { useSearchParam, asString } from '../../url/useSearchParam';
import type { ProjectStatus } from './projects';

// "Show just the projects at one lifecycle status" — a self-contained control that owns the
// `projectStatus` URL key. The projects list reads the same key and keeps only projects whose
// status matches; the two share only the key, no import. Choosing a status commits it to the URL
// (so the narrowed view outlives the click and is shareable); the empty option clears the key.
const STATUSES: { value: ProjectStatus; label: string }[] = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'ON_HOLD', label: 'On hold' },
  { value: 'FINISHED', label: 'Finished' },
];

function StatusFilter() {
  const [status, setStatus] = useSearchParam('projectStatus', asString);
  return (
    <select
      data-testid="project-status-filter"
      value={status}
      onChange={(e) => setStatus(e.target.value)}
    >
      <option value="">All statuses</option>
      {STATUSES.map((s) => (
        <option key={s.value} value={s.value}>
          {s.label}
        </option>
      ))}
    </select>
  );
}

export const contribution = ProjectsToolbar.fill({ order: 30, Component: StatusFilter });
