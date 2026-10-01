import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';

// A task on a project's checklist. Everything that shows a project's tasks shares the key
// ['tasks', projectId]; invalidating it after a toggle refreshes the list from the server.
export type Task = { id: number; projectId: number; title: string; done: boolean };

export const tasksKey = (projectId: number) => ['tasks', projectId] as const;

export function useTasks(projectId: number) {
  return useQuery({
    queryKey: tasksKey(projectId),
    queryFn: () => getJson<Task[]>(`/api/tasks?projectId=${projectId}`),
  });
}

// Tick a task off (or back on): POSTs {id} and invalidates ['tasks', projectId] so the list reflects
// the new done flag from the server. The server also keeps an audit record of every toggle.
export function useToggleTask(projectId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number }) =>
      postJson<Task>('/api/tasks/toggle', { id: input.id }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: tasksKey(projectId) }),
  });
}
