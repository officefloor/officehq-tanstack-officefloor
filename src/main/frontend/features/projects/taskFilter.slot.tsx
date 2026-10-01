import { ProjectTasksToolbar } from '../../slots/defs/projectTasksToolbar';
import { useSearchParam, asString } from '../../url/useSearchParam';

// "Show just the open ones, or just the finished ones" — a self-contained control that owns the
// `taskFilter` URL key. The task list reads the same key and keeps only matching rows; the two share
// only the key, no import. Choosing a state commits it to the URL (so the narrowed view outlives the
// click and is shareable); the empty option clears the key and shows every task again.
const STATES = ['OPEN', 'DONE'] as const;

function TaskFilter() {
  const [filter, setFilter] = useSearchParam('taskFilter', asString);
  return (
    <select
      data-testid="task-filter"
      value={filter}
      onChange={(e) => setFilter(e.target.value)}
    >
      <option value="">All tasks</option>
      {STATES.map((state) => (
        <option key={state} value={state}>
          {state}
        </option>
      ))}
    </select>
  );
}

export const contribution = ProjectTasksToolbar.fill({ order: 10, Component: TaskFilter });
