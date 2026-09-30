import { getJson, postJson } from '../../api/http';

// Server data under ONE shared key: anything showing projects reads ['projects'], and a write
// invalidates the same key to refresh them all (CLAUDE.md rule 5). A project's row carries its
// client's NAME (the cross-entity join is done server-side, in ProjectView).
export type ProjectView = {
  id: number;
  name: string;
  clientName: string;
  archived: boolean;
  tagIds: number[];
};

export const projectsKey = ['projects'] as const;

// The URL search-param key the show-archived toggle owns and the list reads (CLAUDE.md rule 4).
// Shared as a constant so the two files agree on the one key without importing each other.
export const PROJECTS_SHOW_ARCHIVED_PARAM = 'showArchived';

// The URL search-param key the label filter owns and the list reads (CLAUDE.md rule 4): the chosen
// tag id, or absent for "all". Shared as a constant so the two files agree on the one key.
export const PROJECTS_TAG_FILTER_PARAM = 'tag';

// The shared pool of labels, read under the SHARED ['tags'] key (CLAUDE.md rule 5) — the projects
// filter and the project-detail label picker stay in step through the key, without importing each
// other: attaching a label elsewhere invalidates ['tags'] and this select updates itself.
export type TagOption = { id: number; name: string };

export const tagsKey = ['tags'] as const;

export function fetchTagOptions(): Promise<TagOption[]> {
  return getJson<TagOption[]>('/api/tags');
}

export function fetchProjects(): Promise<ProjectView[]> {
  return getJson<ProjectView[]>('/api/projects');
}

export function createProject(input: { name: string; clientId: number }): Promise<ProjectView> {
  return postJson<ProjectView>('/api/projects', input);
}

/** Delete a project the user no longer needs; the id is in the path, no body needed. */
export function deleteProject(projectId: number): Promise<void> {
  return postJson<void>(`/api/projects/${projectId}/delete`, {});
}

/** Archive (tuck away) a project so it drops off the lists but is retained; id is in the path. */
export function archiveProject(projectId: number): Promise<void> {
  return postJson<void>(`/api/projects/${projectId}/archive`, {});
}

// The client options the form's select needs. Read under the SHARED ['clients'] key (CLAUDE.md
// rule 5) — features never import each other, they stay in step through the key: creating a client
// elsewhere invalidates ['clients'] and this select updates itself.
export type ClientOption = { id: number; name: string };

export const clientsKey = ['clients'] as const;

export function fetchClientOptions(): Promise<ClientOption[]> {
  return getJson<ClientOption[]>('/api/clients');
}
