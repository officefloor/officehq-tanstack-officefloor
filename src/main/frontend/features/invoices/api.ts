import { getJson, postJson } from '../../api/http';

// An invoice as the API exposes it — an amount belonging to a project. The query key is scoped to
// the project, ['invoices', projectId], so each project's detail page reads (and invalidates) only
// its own invoices: the list reads the key, the add form invalidates it.
export type Invoice = { id: number; projectId: number; amount: number };
export type NewInvoice = { projectId: number; amount: number };

export const invoicesKey = (projectId: number) => ['invoices', projectId] as const;

export const listInvoices = (projectId: number): Promise<Invoice[]> =>
  getJson<Invoice[]>(`/api/invoices?projectId=${projectId}`);

export const createInvoice = (body: NewInvoice): Promise<Invoice> =>
  postJson<Invoice>('/api/invoices', body);
