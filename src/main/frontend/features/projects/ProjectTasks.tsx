import { useTasks, useToggleTask } from './tasks';

// A project's task checklist. Reads its own query key (scoped to the project) and shows each task's
// title and status (OPEN while not done, DONE once ticked off) with a control to toggle it. A toggle
// is a mutation that invalidates the shared key, so the row re-reads its status from the server.
export function ProjectTasks({ projectId }: { projectId: number }) {
  const { data: tasks } = useTasks(projectId);
  const toggle = useToggleTask(projectId);

  if (!tasks) {
    return null;
  }

  if (tasks.length === 0) {
    return <p data-testid="project-tasks-empty">No tasks yet.</p>;
  }

  return (
    <table data-testid="project-tasks-table">
      <thead>
        <tr>
          <th>Task</th>
          <th>Status</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {tasks.map((task) => (
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
  );
}
