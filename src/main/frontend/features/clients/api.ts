import { getJson, postJson } from '../../api/http';

// A client as the API exposes it. The query key ['clients'] is the shared handle: the list reads
// it, the create form invalidates it, and any future feature showing clients joins by reusing it.
export type Client = { id: number; name: string; email: string };
export type NewClient = { name: string; email: string };

export const clientsKey = ['clients'] as const;

export const listClients = (): Promise<Client[]> => getJson<Client[]>('/api/clients');

export const createClient = (body: NewClient): Promise<Client> =>
  postJson<Client>('/api/clients', body);

// A project of one client as the API exposes it — the same shape the projects list renders, so the
// client detail panel reuses the project-row-<id>/project-name anchors. The query key is scoped to
// the client, ['clients', clientId, 'projects'], so each client's detail page reads only its own
// projects; sharing the ['clients'] prefix means a client write can invalidate it too.
export type ClientProject = { id: number; name: string; clientId: number; clientName: string };

export const clientProjectsKey = (clientId: number) =>
  ['clients', clientId, 'projects'] as const;

export const listClientProjects = (clientId: number): Promise<ClientProject[]> =>
  getJson<ClientProject[]>(`/api/clients/projects?clientId=${clientId}`);
