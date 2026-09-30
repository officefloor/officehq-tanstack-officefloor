import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';

// A task as the server returns it: a title and a done flag that can be ticked off, plus the id of
// the project it belongs to. done=false shows as OPEN, done=true as DONE.
export type Task = {
  id: number;
  projectId: number;
  title: string;
  done: boolean;
};

// A project's tasks, rendered in the project-detail context: the list of that project's tasks plus a
// form to add one and a per-row toggle to tick it off. Server data is read with useQuery under the
// ['tasks'] key and filtered to the project this panel is handed — never copied into state, never
// hand-maintained (rule 5). Adding and toggling are useMutations that invalidate ['tasks'], so the
// list refetches itself. The only useState here is the title the user is currently typing (rule 4).
export function ProjectTasksPanel({ projectId }: { projectId: number }) {
  const queryClient = useQueryClient();
  const tasks = useQuery({
    queryKey: ['tasks'],
    queryFn: () => getJson<Task[]>('/api/tasks'),
  });

  const [title, setTitle] = useState('');

  const create = useMutation({
    mutationFn: () => postJson<Task>('/api/tasks', { projectId, title }),
    onSuccess: () => {
      setTitle('');
      void queryClient.invalidateQueries({ queryKey: ['tasks'] });
    },
  });

  const toggle = useMutation({
    mutationFn: (id: number) => postJson<Task>('/api/tasks/toggle', { id }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['tasks'] });
    },
  });

  const rows = (tasks.data ?? []).filter((t) => t.projectId === projectId);

  return (
    <section data-testid="project-tasks">
      <form
        data-testid="task-form"
        onSubmit={(e) => {
          e.preventDefault();
          if (!title.trim()) {
            return;
          }
          create.mutate();
        }}
      >
        <input
          data-testid="task-form-title"
          placeholder="Task title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />
        <button data-testid="task-form-submit" type="submit">
          Add task
        </button>
      </form>

      {rows.length === 0 ? (
        <p data-testid="project-tasks-empty">No tasks yet.</p>
      ) : (
        <table data-testid="project-tasks-table">
          <thead>
            <tr>
              <th>Task</th>
              <th>Status</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {rows.map((t) => (
              <tr key={t.id} data-testid={`task-row-${t.id}`}>
                <td data-testid="task-title">{t.title}</td>
                <td data-testid="task-status">{t.done ? 'DONE' : 'OPEN'}</td>
                <td>
                  <button
                    type="button"
                    data-testid={`task-toggle-${t.id}`}
                    onClick={() => toggle.mutate(t.id)}
                  >
                    {t.done ? 'Reopen' : 'Tick off'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
