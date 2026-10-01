import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';

// A label that groups projects. Everything showing a project's tags shares the key
// ['project-tags', projectId]; the full catalogue of tags (for the add picker) shares ['tags'].
// Invalidating ['project-tags', projectId] after an add/remove refreshes the list from the server.
export type Tag = { id: number; name: string };

export const projectTagsKey = (projectId: number) => ['project-tags', projectId] as const;
export const tagsKey = () => ['tags'] as const;

export function useProjectTags(projectId: number) {
  return useQuery({
    queryKey: projectTagsKey(projectId),
    queryFn: () => getJson<Tag[]>(`/api/project-tags?projectId=${projectId}`),
  });
}

export function useAllTags() {
  return useQuery({
    queryKey: tagsKey(),
    queryFn: () => getJson<Tag[]>('/api/tags'),
  });
}

// Put a tag on the project: POSTs {projectId, tagId} and invalidates ['project-tags', projectId] so
// the chip list re-reads from the server. The server also audits every tagging.
export function useAddProjectTag(projectId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { tagId: number }) =>
      postJson<Tag[]>('/api/project-tags/add', { projectId, tagId: input.tagId }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: projectTagsKey(projectId) }),
  });
}

// Take a tag off the project, same shared key so the chip list refreshes. The server audits it too.
export function useRemoveProjectTag(projectId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { tagId: number }) =>
      postJson<Tag[]>('/api/project-tags/remove', { projectId, tagId: input.tagId }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: projectTagsKey(projectId) }),
  });
}
