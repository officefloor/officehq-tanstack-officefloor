import { getJson, postJson } from '../../api/http';

// Server data under ONE shared key: anything showing clients reads ['clients'], and a write
// invalidates the same key to refresh them all (CLAUDE.md rule 5).
export type Client = { id: number; name: string; email: string };

export const clientsKey = ['clients'] as const;

// The URL search-param key the search box owns and the list reads (CLAUDE.md rule 4). Shared as a
// constant so the two files agree on the one key without importing each other's components.
export const CLIENT_SEARCH_PARAM = 'clientSearch';

// Case-insensitive match of a client's name against the current search text. An empty box matches
// every client, so the unfiltered list shows.
export function matchesClientSearch(client: Client, query: string): boolean {
  return client.name.toLowerCase().includes(query.trim().toLowerCase());
}

// A "proper email address": one @, non-empty local and domain parts, and a dotted domain. Shared by
// the form and mirrored by the server so a malformed address can never be saved.
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function isValidEmail(email: string): boolean {
  return EMAIL_PATTERN.test(email.trim());
}

export function fetchClients(): Promise<Client[]> {
  return getJson<Client[]>('/api/clients');
}

// A client's own projects, under a key nested beneath ['clients'] so invalidating clients refreshes
// them too (CLAUDE.md rule 5). The row only needs the project's id and name; the server-side join
// still supplies a client name we simply don't render here.
export type ClientProject = { id: number; name: string };

export const clientProjectsKey = (clientId: number) =>
  ['clients', clientId, 'projects'] as const;

export function fetchClientProjects(clientId: number): Promise<ClientProject[]> {
  return getJson<ClientProject[]>(`/api/clients/${clientId}/projects`);
}

export function createClient(input: { name: string; email: string }): Promise<Client> {
  return postJson<Client>('/api/clients', input);
}
