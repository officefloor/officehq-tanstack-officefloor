import { getJson, postJson } from '../../api/http';

// A client as the API exposes it. The query key ['clients'] is the shared handle: the list reads
// it, the create form invalidates it, and any future feature showing clients joins by reusing it.
export type Client = { id: number; name: string; email: string };
export type NewClient = { name: string; email: string };

export const clientsKey = ['clients'] as const;

export const listClients = (): Promise<Client[]> => getJson<Client[]>('/api/clients');

export const createClient = (body: NewClient): Promise<Client> =>
  postJson<Client>('/api/clients', body);

// Archive a client — tucks it away server-side (keeps the row, sets its archived flag) and returns
// the updated client. Callers invalidate ['clients'] on success so the list refetches and the row
// drops off both the list and the search.
export const archiveClient = (id: number): Promise<Client> =>
  postJson<Client>('/api/clients/archive', { id });

// A project of one client as the API exposes it — the same shape the projects list renders, so the
// client detail panel reuses the project-row-<id>/project-name anchors. The query key is scoped to
// the client, ['clients', clientId, 'projects'], so each client's detail page reads only its own
// projects; sharing the ['clients'] prefix means a client write can invalidate it too.
export type ClientProject = { id: number; name: string; clientId: number; clientName: string };

export const clientProjectsKey = (clientId: number) =>
  ['clients', clientId, 'projects'] as const;

export const listClientProjects = (clientId: number): Promise<ClientProject[]> =>
  getJson<ClientProject[]>(`/api/clients/projects?clientId=${clientId}`);

// A contact the user keeps for a client — a name, email and role. The query key is scoped to the
// client, ['clients', clientId, 'contacts'], so each client's detail page reads (and invalidates)
// only its own contacts: the list reads the key, the add form invalidates it. Sharing the
// ['clients'] prefix means a client write can invalidate it too.
export type Contact = {
  id: number;
  name: string;
  email: string;
  role: string;
  clientId: number;
};
export type NewContact = { clientId: number; name: string; email: string; role: string };

export const clientContactsKey = (clientId: number) =>
  ['clients', clientId, 'contacts'] as const;

export const listClientContacts = (clientId: number): Promise<Contact[]> =>
  getJson<Contact[]>(`/api/clients/contacts?clientId=${clientId}`);

export const createContact = (body: NewContact): Promise<Contact> =>
  postJson<Contact>('/api/clients/contacts', body);

// At-a-glance counts for one client: how many projects it owns and how many contacts it keeps.
// Computed server-side (GET /api/clients/summary). The query key is scoped to the client under the
// ['clients', clientId, ...] prefix, so a client write can invalidate it alongside the lists it
// summarises — the badges stay in step with the projects and contacts panels for free.
export type ClientSummary = { projects: number; contacts: number };

export const clientSummaryKey = (clientId: number) =>
  ['clients', clientId, 'summary'] as const;

export const getClientSummary = (clientId: number): Promise<ClientSummary> =>
  getJson<ClientSummary>(`/api/clients/summary?clientId=${clientId}`);
