import { getJson, postJson } from '../../api/http';

// A project's tasks under a key nested beneath ['projects'] so invalidating projects refreshes them
// too (CLAUDE.md rule 5). Each task carries a title and a done flag.
export type Task = { id: number; projectId: number; title: string; done: boolean };

export const projectTasksKey = (projectId: number) =>
  ['projects', projectId, 'tasks'] as const;

// The URL key the "open vs finished" filter owns (CLAUDE.md rule 4). '' = all tasks, 'OPEN' = only
// not-done, 'DONE' = only done. The control (filter.slot.tsx) writes it; the table reads the same
// key to drop rows at the other status — no callback, no shared state, just the key.
export const TASK_FILTER_PARAM = 'taskFilter';
export const TASK_FILTERS = ['OPEN', 'DONE'] as const;

export function fetchProjectTasks(projectId: number): Promise<Task[]> {
  return getJson<Task[]>(`/api/projects/${projectId}/tasks`);
}

/** Tick a task off (or back on); the server flips its done flag and records the audited side-effect. */
export function toggleTask(taskId: number): Promise<Task> {
  return postJson<Task>(`/api/tasks/${taskId}/toggle`, {});
}
