import { getJson, postJson } from '../../api/http';

// A project's invoices under a per-project key: anything showing project 1's invoices reads
// ['invoices', 1], and a write invalidates the same key to refresh them (CLAUDE.md rule 5). An
// invoice carries its own id, its project's id and a monetary amount.
export type Invoice = {
  id: number;
  projectId: number;
  amount: number;
  status: string;
  issuedDate: string;
  dueDate: string;
};

export const invoicesKey = (projectId: number) => ['invoices', projectId] as const;

/** URL key owned by the sort control; readers (the list) sort by the same key (CLAUDE.md rule 4). */
export const INVOICE_SORT_PARAM = 'invoiceSort';

export function fetchInvoices(projectId: number): Promise<Invoice[]> {
  return getJson<Invoice[]>(`/api/invoices/${projectId}`);
}

export function createInvoice(input: { projectId: number; amount: number }): Promise<Invoice> {
  return postJson<Invoice>('/api/invoices', input);
}

/** Mark an invoice paid; the server flips its status and records the audited side-effect. */
export function payInvoice(invoiceId: number): Promise<Invoice> {
  return postJson<Invoice>(`/api/invoices/${invoiceId}/pay`, {});
}
