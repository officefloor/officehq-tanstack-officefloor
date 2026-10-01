import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';

// A note written against a project. Everything that shows a project's notes shares the key
// ['project', projectId, 'notes']; writing one invalidates it so the list re-reads from the server.
// The server returns notes newest first and stamps each new one with the current instant, so a
// freshly written note sorts on top.
export type Note = {
  id: number;
  targetType: string;
  targetId: number;
  text: string;
  at: string;
};

export const projectNotesKey = (projectId: number) =>
  ['project', projectId, 'notes'] as const;

export function useProjectNotes(projectId: number) {
  return useQuery({
    queryKey: projectNotesKey(projectId),
    queryFn: () =>
      getJson<Note[]>(`/api/notes?targetType=project&targetId=${projectId}`),
  });
}

export function useCreateProjectNote(projectId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { text: string }) =>
      postJson<Note>('/api/notes', {
        targetType: 'project',
        targetId: projectId,
        text: input.text,
      }),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: projectNotesKey(projectId) }),
  });
}
