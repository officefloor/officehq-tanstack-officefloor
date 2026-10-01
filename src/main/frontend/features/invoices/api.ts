import { getJson, postJson } from '../../api/http';

// An invoice as the API exposes it — an amount belonging to a project. The query key is scoped to
// the project, ['invoices', projectId], so each project's detail page reads (and invalidates) only
// its own invoices: the list reads the key, the add form invalidates it.
export type Invoice = {
  id: number;
  projectId: number;
  amount: number;
  status: string;
  issuedDate: string;
  dueDate: string;
  // The currency the invoice's client is billed in — the amount is shown in it (a short ISO code).
  currency: string;
};
export type NewInvoice = { projectId: number; amount: number };

// The base key for a project's invoices. The list query appends its current sort below it, so the
// 'id' and 'due' orderings cache separately — while a write still invalidates ['invoices', id] and,
// by prefix, refreshes whichever ordering is on screen.
export const invoicesKey = (projectId: number) => ['invoices', projectId] as const;

// How to order the list: earliest-due-first ('due') or id order (the default). The server reads the
// same `sort` key, so the ordering is applied once, at the source, not re-sorted on the client.
export type InvoiceSort = 'id' | 'due';

export const listInvoices = (projectId: number, sort: InvoiceSort = 'id'): Promise<Invoice[]> =>
  getJson<Invoice[]>(`/api/invoices?projectId=${projectId}&sort=${sort}`);

// One invoice, with its status WORKED OUT from its payments (still owing / part paid / paid). The
// detail page reads this to show the derived status; it is recomputed server-side from the payments.
export const getInvoice = (invoiceId: number): Promise<Invoice> =>
  getJson<Invoice>(`/api/invoices/one?invoiceId=${invoiceId}`);

export const createInvoice = (body: NewInvoice): Promise<Invoice> =>
  postJson<Invoice>('/api/invoices', body);

// Send an invoice — flips DRAFT -> SENT server-side and appends the audit record. Callers invalidate
// ['invoices', projectId] on success so the invoices panel (and the row actions) refetch the status.
export const sendInvoice = (id: number): Promise<Invoice> =>
  postJson<Invoice>('/api/invoices/send', { id });

// Cancel an invoice sent by mistake — flips it to VOID server-side and appends the audit record. A
// VOID invoice stops counting towards what is owed. Callers invalidate ['invoices', projectId] (and
// ['dashboard']) on success so the invoices panel and the home figures refetch.
export const cancelInvoice = (id: number): Promise<Invoice> =>
  postJson<Invoice>('/api/invoices/cancel', { id });
