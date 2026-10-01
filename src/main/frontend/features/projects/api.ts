import { getJson, postJson } from '../../api/http';

// A project as the API exposes it — it carries its client's NAME (the cross-entity join done on the
// server) so the list shows the client without a second lookup. The query key ['projects'] is the
// shared handle: the list reads it, the create form invalidates it.
export type Project = {
  id: number;
  name: string;
  clientId: number;
  clientName: string;
  archived: boolean;
  // Where the project sits in its lifecycle: ACTIVE, ON_HOLD or FINISHED. The row shows it; the
  // create form sets it.
  status: ProjectStatus;
  // The short reference code set when the project was created — shown on the row. Unique across
  // projects. Null only for projects that predate codes.
  code: string | null;
  tagIds: number[];
};
export type ProjectStatus = 'ACTIVE' | 'ON_HOLD' | 'FINISHED';
export const projectStatuses: ProjectStatus[] = ['ACTIVE', 'ON_HOLD', 'FINISHED'];
export type NewProject = {
  name: string;
  clientId: number;
  status: ProjectStatus;
  code: string;
};

export const projectsKey = ['projects'] as const;

export const listProjects = (): Promise<Project[]> => getJson<Project[]>('/api/projects');

export const createProject = (body: NewProject): Promise<Project> =>
  postJson<Project>('/api/projects', body);

// Archive a project — tucks it away server-side (keeps the row, sets its archived flag) and returns
// the updated project. Callers invalidate ['projects'] on success so the list refetches and the row
// drops off, unless the archived ones are being shown.
export const archiveProject = (id: number): Promise<Project> =>
  postJson<Project>('/api/projects/archive', { id });

// The client options the form's select needs: shared with the clients feature by the ['clients']
// KEY (not an import), so adding a client refreshes this select too.
export type ClientOption = { id: number; name: string };
export const clientsKey = ['clients'] as const;
export const listClientOptions = (): Promise<ClientOption[]> =>
  getJson<ClientOption[]>('/api/clients');
