import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ProjectDetail } from '../../slots/defs/projectDetail';
import { tasksKey, listTasks, toggleTask, type Task } from './tasksApi';

// A project's task list, with a tick-off control on each row — one panel filling the project.detail
// region. Reads server data under ['tasks', projectId] (never copied into state); the toggle is a
// useMutation that, on success, invalidates the same key so this panel refetches the new status. The
// OPEN/DONE label is DERIVED from each row's `done` flag, never stored.
function ProjectTasks({ projectId }: { projectId: number }) {
  const queryClient = useQueryClient();
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
        {tasks.map((task: Task) => (
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
