import { ProjectTasksToolbar } from '../../slots/defs/projectTasksToolbar';
import { useSearchParam, asString } from '../../url/useSearchParam';
import { useTasks, useToggleTask } from './tasks';

// A project's task checklist. Reads its own query key (scoped to the project) and shows each task's
// title and status (OPEN while not done, DONE once ticked off) with a control to toggle it. A toggle
// is a mutation that invalidates the shared key, so the row re-reads its status from the server.
// When the shared `taskFilter` URL key (set by a toolbar control) names a state, the list keeps only
// the open tasks (OPEN) or only the finished ones (DONE).
export function ProjectTasks({ projectId }: { projectId: number }) {
  const { data: tasks } = useTasks(projectId);
  const toggle = useToggleTask(projectId);
  const [filter] = useSearchParam('taskFilter', asString);

  if (!tasks) {
    return null;
  }

  if (tasks.length === 0) {
    return <p data-testid="project-tasks-empty">No tasks yet.</p>;
  }

  const shown =
    filter === 'OPEN'
      ? tasks.filter((task) => !task.done)
      : filter === 'DONE'
        ? tasks.filter((task) => task.done)
        : tasks;

  return (
    <>
    <ProjectTasksToolbar.Slot />
    <table data-testid="project-tasks-table">
      <thead>
        <tr>
          <th>Task</th>
          <th>Status</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {shown.map((task) => (
          <tr key={task.id} data-testid={`task-row-${task.id}`}>
            <td data-testid="task-title">{task.title}</td>
            <td data-testid="task-status">{task.done ? 'DONE' : 'OPEN'}</td>
            <td>
              <button
                type="button"
                data-testid={`task-toggle-${task.id}`}
                disabled={toggle.isPending}
                onClick={() => toggle.mutate({ id: task.id })}
              >
                {task.done ? 'Reopen' : 'Tick off'}
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
    </>
  );
}
