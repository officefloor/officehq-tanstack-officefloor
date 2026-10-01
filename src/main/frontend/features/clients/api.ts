import { getJson, postJson } from '../../api/http';

// A client as the API exposes it. The query key ['clients'] is the shared handle: the list reads
// it, the create form invalidates it, and any future feature showing clients joins by reusing it.
export type Client = { id: number; name: string; email: string };
export type NewClient = { name: string; email: string };

export const clientsKey = ['clients'] as const;

export const listClients = (): Promise<Client[]> => getJson<Client[]>('/api/clients');

export const createClient = (body: NewClient): Promise<Client> =>
  postJson<Client>('/api/clients', body);
