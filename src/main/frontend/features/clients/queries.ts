import { getJson, postJson } from '../../api/http';

// Server data under ONE shared key: anything showing clients reads ['clients'], and a write
// invalidates the same key to refresh them all (CLAUDE.md rule 5).
export type Client = { id: number; name: string; email: string };

export const clientsKey = ['clients'] as const;

export function fetchClients(): Promise<Client[]> {
  return getJson<Client[]>('/api/clients');
}

export function createClient(input: { name: string; email: string }): Promise<Client> {
  return postJson<Client>('/api/clients', input);
}
