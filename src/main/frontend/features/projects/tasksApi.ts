import { getJson, postJson } from '../../api/http';

// A task as the API exposes it — a to-do item belonging to a project. The query key is scoped to the
// project, ['tasks', projectId], so each project's detail page reads (and invalidates) only its own
// tasks: the list reads the key, a toggle invalidates it. `done` is the raw tick-off flag; the panel
// maps it to the OPEN/DONE label the UI shows.
export type Task = { id: number; projectId: number; title: string; done: boolean };

export const tasksKey = (projectId: number) => ['tasks', projectId] as const;

export const listTasks = (projectId: number): Promise<Task[]> =>
  getJson<Task[]>(`/api/tasks?projectId=${projectId}`);

// Tick a task off (or back on) — flips its done flag server-side and returns the updated row. Callers
// invalidate ['tasks', projectId] on success so the tasks panel refetches the new status.
export const toggleTask = (id: number): Promise<Task> =>
  postJson<Task>('/api/tasks/toggle', { id });
