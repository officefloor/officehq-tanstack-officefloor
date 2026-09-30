import { getJson, postJson } from '../../api/http';

// A project's labels under a per-project key nested beneath ['projects'] so invalidating a project
// refreshes its chips too (CLAUDE.md rule 5). The shared pool of labels sits under its own ['tags']
// key. A tag is just an id and a name.
export type Tag = { id: number; name: string };

export const projectTagsKey = (projectId: number) =>
  ['projects', projectId, 'tags'] as const;

export const allTagsKey = ['tags'] as const;

export function fetchProjectTags(projectId: number): Promise<Tag[]> {
  return getJson<Tag[]>(`/api/projects/${projectId}/tags`);
}

export function fetchAllTags(): Promise<Tag[]> {
  return getJson<Tag[]>('/api/tags');
}

/** Attach a label to a project; the project id is in the path, the tag id is the body. */
export function addProjectTag(projectId: number, tagId: number): Promise<Tag[]> {
  return postJson<Tag[]>(`/api/projects/${projectId}/tags`, { tagId });
}

/** Detach a label from a project; both ids are in the path, no body needed. */
export function removeProjectTag(projectId: number, tagId: number): Promise<Tag[]> {
  return postJson<Tag[]>(`/api/projects/${projectId}/tags/${tagId}/remove`, {});
}
