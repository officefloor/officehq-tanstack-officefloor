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

// Every invoice across all projects, each carrying the NAME of its project (the cross-entity join
// is done server-side, in AllInvoicesGet). Read under ['invoices', 'all'] (CLAUDE.md rule 5).
export type AllInvoice = {
  id: number;
  projectId: number;
  projectName: string;
  amount: number;
  status: string;
};

export const allInvoicesKey = ['invoices', 'all'] as const;

export function fetchAllInvoices(): Promise<AllInvoice[]> {
  return getJson<AllInvoice[]>('/api/invoices');
}

/** URL key owned by the sort control; readers (the list) sort by the same key (CLAUDE.md rule 4). */
export const INVOICE_SORT_PARAM = 'invoiceSort';

/** URL key owned by the stage filter; the all-invoices list narrows to this stage (CLAUDE.md rule 4). */
export const INVOICE_STATUS_PARAM = 'invoiceStatus';

/** The lifecycle stages an invoice can sit at — the options the stage filter offers. */
export const INVOICE_STATUSES = ['DRAFT', 'SENT', 'PAID'] as const;

export function fetchInvoices(projectId: number): Promise<Invoice[]> {
  return getJson<Invoice[]>(`/api/invoices/${projectId}`);
}

export function createInvoice(input: { projectId: number; amount: number }): Promise<Invoice> {
  return postJson<Invoice>('/api/invoices', input);
}

/** Send a draft invoice; the server moves it to SENT and records the audited side-effect. */
export function sendInvoice(invoiceId: number): Promise<Invoice> {
  return postJson<Invoice>(`/api/invoices/${invoiceId}/send`, {});
}

/** Mark an invoice paid; the server flips its status and records the audited side-effect. */
export function payInvoice(invoiceId: number): Promise<Invoice> {
  return postJson<Invoice>(`/api/invoices/${invoiceId}/pay`, {});
}
