import { ProjectDetail } from '../../slots/defs/projectDetail';
import { useSearchParam, asString } from '../../url/useSearchParam';

// A control over the project's task list — one new *.slot.tsx filling the project.detail region. It
// owns the `taskFilter` URL key; the tasks panel reads the same key and shows only the matching
// rows. Nothing is passed between them: they stay in step through the shared search param. The empty
// option clears the key, so the list falls back to showing every task.
function TaskFilter() {
  const [filter, setFilter] = useSearchParam('taskFilter', asString);
  return (
    <select
      data-testid="task-filter"
      value={filter}
      onChange={(e) => setFilter(e.target.value)}
    >
      <option value="">All tasks</option>
      <option value="OPEN">OPEN</option>
      <option value="DONE">DONE</option>
    </select>
  );
}

export const contribution = ProjectDetail.fill({ order: 25, Component: TaskFilter });
