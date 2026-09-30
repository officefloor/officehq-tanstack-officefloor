import { ProjectDetail } from '../../slots/defs/projectDetail';
import { asString, useSearchParam } from '../../url/useSearchParam';

// The open/done task filter — its own file, filling the project-detail slot above the tasks panel
// (order 15, the panel is 20). It OWNS the `taskFilter` URL key (rule 4: a filter outlives a click,
// so it lives in the URL, not useState). The tasks panel reads the same key and narrows its rows;
// nothing is passed between them. Carries data-testid="task-filter" (the test contract). The empty
// option clears the key, showing every task again.
function TaskFilter() {
  const [filter, setFilter] = useSearchParam('taskFilter', asString);
  return (
    <select
      data-testid="task-filter"
      value={filter}
      onChange={(e) => setFilter(e.target.value || undefined)}
    >
      <option value="">All tasks</option>
      <option value="OPEN">Open</option>
      <option value="DONE">Done</option>
    </select>
  );
}

export const contribution = ProjectDetail.fill({ order: 15, Component: TaskFilter });
