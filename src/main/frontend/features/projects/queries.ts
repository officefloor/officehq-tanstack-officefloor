import { getJson, postJson } from '../../api/http';

// Server data under ONE shared key: anything showing projects reads ['projects'], and a write
// invalidates the same key to refresh them all (CLAUDE.md rule 5). A project's row carries its
// client's NAME (the cross-entity join is done server-side, in ProjectView).
export type ProjectView = { id: number; name: string; clientName: string };

export const projectsKey = ['projects'] as const;

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

// The client options the form's select needs. Read under the SHARED ['clients'] key (CLAUDE.md
// rule 5) — features never import each other, they stay in step through the key: creating a client
// elsewhere invalidates ['clients'] and this select updates itself.
export type ClientOption = { id: number; name: string };

export const clientsKey = ['clients'] as const;

export function fetchClientOptions(): Promise<ClientOption[]> {
  return getJson<ClientOption[]>('/api/clients');
}
