import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  projectTasksKey,
  fetchProjectTasks,
  toggleTask,
  TASK_FILTER_PARAM,
  type Task,
} from './queries';
import { ProjectTasksToolbar } from '../../slots/defs/projectTasksToolbar';
import { useSearchParam, asString } from '../../url/useSearchParam';

// A project's task checklist. Queries for itself under ['projects', projectId, 'tasks'] — never
// handed its data by a parent (CLAUDE.md rule 5). Ticking a task off is a mutation that invalidates
// the same key so the row (and its status cell) refresh themselves (rule 5); the audited record is
// written server-side. Carries the stable data-testid contract: the table, a row per task, the
// title/status cells and the per-row toggle button the test reads.
export function ProjectTasksTable({ projectId }: { projectId: number }) {
  const queryClient = useQueryClient();
  const { data: tasks } = useQuery({
    queryKey: projectTasksKey(projectId),
    queryFn: () => fetchProjectTasks(projectId),
  });
  const mutation = useMutation({
    mutationFn: (taskId: number) => toggleTask(taskId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: projectTasksKey(projectId) });
    },
  });

  const [filter] = useSearchParam(TASK_FILTER_PARAM, asString);

  if (!tasks) {
    return null;
  }

  // Reads the shared `taskFilter` key (rule 4) to show just the open, just the finished, or all
  // tasks. The filter control (filter.slot.tsx) writes the same key.
  const shown =
    filter === 'OPEN'
      ? tasks.filter((task: Task) => !task.done)
      : filter === 'DONE'
        ? tasks.filter((task: Task) => task.done)
        : tasks;

  return (
    <>
    <ProjectTasksToolbar.Slot />
    <table data-testid="project-tasks-table">
      <thead>
        <tr>
          <th>Task</th>
          <th>Status</th>
          <th />
        </tr>
      </thead>
      <tbody>
        {shown.map((task: Task) => (
          <tr key={task.id} data-testid={`task-row-${task.id}`}>
            <td data-testid="task-title">{task.title}</td>
            <td data-testid="task-status">{task.done ? 'DONE' : 'OPEN'}</td>
            <td>
              <button
                data-testid={`task-toggle-${task.id}`}
                type="button"
                onClick={() => mutation.mutate(task.id)}
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
