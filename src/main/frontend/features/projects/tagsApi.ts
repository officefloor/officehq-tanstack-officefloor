import { getJson, postJson } from '../../api/http';

// A tag as the API exposes it — a reusable label used to group projects. The catalogue of all tags
// lives under ['tags'] (the add-tag select reads it); the tags ON one project live under
// ['projectTags', projectId] (the chips read it, add/remove invalidate it), scoped so each project's
// detail page refreshes only its own tags.
export type Tag = { id: number; name: string };

export const tagsKey = ['tags'] as const;
export const projectTagsKey = (projectId: number) => ['projectTags', projectId] as const;

export const listTags = (): Promise<Tag[]> => getJson<Tag[]>('/api/tags');

export const listProjectTags = (projectId: number): Promise<Tag[]> =>
  getJson<Tag[]>(`/api/projects/tags?projectId=${projectId}`);

// Put a tag on a project / take it off — both return the project's tags after the change. Callers
// invalidate ['projectTags', projectId] on success so the chips refetch.
export const addProjectTag = (projectId: number, tagId: number): Promise<Tag[]> =>
  postJson<Tag[]>('/api/projects/tags', { projectId, tagId });

export const removeProjectTag = (projectId: number, tagId: number): Promise<Tag[]> =>
  postJson<Tag[]>('/api/projects/tags/remove', { projectId, tagId });
