import { getJson } from '../../api/http';

// The global search reads the SAME server data the clients and projects lists read — it joins them
// by KEY, never by importing another feature. ['clients'] and ['projects'] are the shared handles:
// a create or archive elsewhere invalidates them and the search refreshes with no import between us.
export type SearchClient = { id: number; name: string };
export type SearchProject = { id: number; name: string };

export const clientsKey = ['clients'] as const;
export const projectsKey = ['projects'] as const;

export const listClients = (): Promise<SearchClient[]> => getJson<SearchClient[]>('/api/clients');
export const listProjects = (): Promise<SearchProject[]> =>
  getJson<SearchProject[]>('/api/projects');
