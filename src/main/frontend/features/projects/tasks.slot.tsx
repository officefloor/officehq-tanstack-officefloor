import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ProjectDetail } from '../../slots/defs/projectDetail';
import { useSearchParam, asString } from '../../url/useSearchParam';
import { tasksKey, listTasks, toggleTask, type Task } from './tasksApi';

// A project's task list, with a tick-off control on each row — one panel filling the project.detail
// region. Reads server data under ['tasks', projectId] (never copied into state); the toggle is a
// useMutation that, on success, invalidates the same key so this panel refetches the new status. The
// OPEN/DONE label is DERIVED from each row's `done` flag, never stored.
//
// The chosen view lives in the URL under the shared `taskFilter` key — the filter control (its own
// *.slot.tsx) writes it, this list reads it and shows only the matching rows. An empty key means
// every task.
function ProjectTasks({ projectId }: { projectId: number }) {
  const queryClient = useQueryClient();
  const [filter] = useSearchParam('taskFilter', asString);
  const { data: tasks } = useQuery({
    queryKey: tasksKey(projectId),
    queryFn: () => listTasks(projectId),
  });
  const mutation = useMutation({
    mutationFn: (id: number) => toggleTask(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: tasksKey(projectId) });
    },
  });

  if (!tasks) {
    return null;
  }

  const visible = tasks.filter((task: Task) =>
    filter === 'OPEN' ? !task.done : filter === 'DONE' ? task.done : true,
  );

  return (
    <table data-testid="project-tasks-table">
      <thead>
        <tr>
          <th>Task</th>
          <th>Status</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {visible.map((task: Task) => (
          <tr key={task.id} data-testid={`task-row-${task.id}`}>
            <td data-testid="task-title">{task.title}</td>
            <td data-testid="task-status">{task.done ? 'DONE' : 'OPEN'}</td>
            <td>
              <button
                data-testid={`task-toggle-${task.id}`}
                type="button"
                disabled={mutation.isPending}
                onClick={() => mutation.mutate(task.id)}
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

export const contribution = ProjectDetail.fill({ order: 30, Component: ProjectTasks });
