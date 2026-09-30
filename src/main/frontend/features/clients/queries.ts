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

// How much each client still owes, keyed by client id — read alongside the clients list so the list
// can be ordered by outstanding amount. Nested under ['clients'] so invalidating clients refreshes
// it too (CLAUDE.md rule 5). The server sums the dues across every invoice raised for the client,
// the same figure the client statement shows.
export type ClientOutstanding = { clientId: number; outstanding: number };

export const clientsOutstandingKey = ['clients', 'outstanding'] as const;

export function fetchClientsOutstanding(): Promise<ClientOutstanding[]> {
  return getJson<ClientOutstanding[]>('/api/clients/outstanding');
}

// The URL search-param key the sort control owns and the list reads (CLAUDE.md rule 4): order the
// clients by 'name' (the default) or by 'outstanding' (how much each owes). Shared as a constant so
// the two files agree on the one key without importing each other's components.
export const CLIENT_SORT_PARAM = 'clientSort';

// A client's own projects, under a key nested beneath ['clients'] so invalidating clients refreshes
// them too (CLAUDE.md rule 5). The row carries the project's lifecycle `status` and `archived` flag
// too, so the client's page can show just the active ones by default and reveal the rest on its
// toggle without a second request. The server-side join also supplies a client name we don't render.
export type ClientProject = {
  id: number;
  name: string;
  status: string;
  archived: boolean;
};

export const clientProjectsKey = (clientId: number) =>
  ['clients', clientId, 'projects'] as const;

export function fetchClientProjects(clientId: number): Promise<ClientProject[]> {
  return getJson<ClientProject[]>(`/api/clients/${clientId}/projects`);
}

// The URL search-param key the "show all projects" toggle owns and the projects table reads
// (CLAUDE.md rule 4). Off by default → only ACTIVE, non-archived projects show; on → the finished
// and hidden (archived) ones show too. Shared as a constant so the two files agree on the one key
// without importing each other's components.
export const CLIENT_PROJECTS_SHOW_ALL_PARAM = 'clientProjectsAll';

// An "active" project — one the user is currently working on: ACTIVE lifecycle status and not
// archived (a tucked-away project is hidden even if still ACTIVE). The client's page shows these by
// default; the toggle reveals the finished and hidden ones.
export function isActiveProject(project: ClientProject): boolean {
  return project.status === 'ACTIVE' && !project.archived;
}

// A client's statement: every invoice raised for the client (across their projects) plus the total
// still owed, under a key nested beneath ['clients'] so invalidating clients refreshes it too
// (CLAUDE.md rule 5). Each invoice carries the money still due (amount - payments), derived
// server-side; the total is the sum of those dues.
//
// The same invoices are also grouped by job in `projects` — one entry per project, each carrying
// that job's own `subtotal` (the sum of its invoices' dues) — so the statement can present the
// invoices under their job with a per-job subtotal. `totalOwed` is unchanged (it still sums every
// due across every job).
export type StatementInvoice = {
  id: number;
  projectId: number;
  amount: number;
  status: string;
  due: number;
};
export type StatementProject = {
  projectId: number;
  projectName: string;
  invoices: StatementInvoice[];
  subtotal: number;
};
export type ClientStatement = {
  projects: StatementProject[];
  invoices: StatementInvoice[];
  totalOwed: number;
};

export const clientStatementKey = (clientId: number) =>
  ['clients', clientId, 'statement'] as const;

export function fetchClientStatement(clientId: number): Promise<ClientStatement> {
  return getJson<ClientStatement>(`/api/clients/${clientId}/statement`);
}

// The URL search-param key the "open statement" control owns and the panel reads (CLAUDE.md rule 4).
export const CLIENT_STATEMENT_PARAM = 'clientStatement';

export function createClient(input: { name: string; email: string }): Promise<Client> {
  return postJson<Client>('/api/clients', input);
}

/** Archive (tuck away) a client so it drops off the list and search but is retained; id is in the
 * path, no body needed. */
export function archiveClient(clientId: number): Promise<void> {
  return postJson<void>(`/api/clients/${clientId}/archive`, {});
}

// The URL search-param key the "edit this client" control owns: which client's row is currently
// being edited (CLAUDE.md rule 4). One key shared by every row's edit control, so opening one row's
// form is a URL change, not per-row component state.
export const CLIENT_EDIT_PARAM = 'editClient';

/** Correct a client's name and email; id is in the path, {name, email} in the body. Returns the
 * saved row. */
export function updateClient(
  clientId: number,
  input: { name: string; email: string },
): Promise<Client> {
  return postJson<Client>(`/api/clients/${clientId}`, input);
}
