import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';

// The shape the server returns (a project joined to its client's name) and the query key everything
// that shows projects shares. Invalidating ['projects'] refreshes every view of projects.
export type Project = { id: number; name: string; clientId: number; clientName: string };

export const projectsKey = ['projects'] as const;

export function useProjects() {
  return useQuery({
    queryKey: projectsKey,
    queryFn: () => getJson<Project[]>('/api/projects'),
  });
}

export function useCreateProject() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { name: string; clientId: number }) =>
      postJson<Project>('/api/projects', input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: projectsKey }),
  });
}

// Delete a project the owner no longer needs: POSTs {id} and returns the id removed. Invalidates
// ['projects'] so every view of projects re-reads from the server and the row drops — the list is
// never hand-edited after the write (CLAUDE.md rule 5). The audit record is written server-side.
export function useDeleteProject() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => postJson<{ id: number }>('/api/projects/remove', { id }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: projectsKey }),
  });
}

// The client choices the project form needs. Reads the SHARED ['clients'] key — no import of the
// clients feature; the two stay in step through the key, not a dependency.
type ClientOption = { id: number; name: string };

export function useClientOptions() {
  return useQuery({
    queryKey: ['clients'] as const,
    queryFn: () => getJson<ClientOption[]>('/api/clients'),
  });
}
