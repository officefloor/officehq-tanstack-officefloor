import { ProjectTasksToolbar } from '../../slots/defs/projectTasksToolbar';
import { useSearchParam, asString } from '../../url/useSearchParam';
import { TASK_FILTER_PARAM, TASK_FILTERS } from './queries';

// The "show just the open ones, or just the finished ones" control — its own file filling the
// project-tasks toolbar region (CLAUDE.md rule 3). It owns the `taskFilter` URL key (rule 4): the
// choice outlives a click, so it lives in the URL, and the checklist (ProjectTasksTable) reads the
// very same key to drop rows at the other status. No callback, no shared state — just the key.
function TaskFilter() {
  const [filter, setFilter] = useSearchParam(TASK_FILTER_PARAM, asString);
  return (
    <select
      data-testid="task-filter"
      value={filter}
      onChange={(e) => setFilter(e.target.value)}
    >
      <option value="">All tasks</option>
      {TASK_FILTERS.map((f) => (
        <option key={f} value={f}>
          {f}
        </option>
      ))}
    </select>
  );
}

export const contribution = ProjectTasksToolbar.fill({ order: 10, Component: TaskFilter });
