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
  /** How much has been settled against this invoice so far (sum of its payments). */
  paid: number;
  /** How much is still owed after payments: amount - paid. Derived server-side. */
  due: number;
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

/** URL key owned by the pagination control; the all-invoices list shows the matching page (CLAUDE.md rule 4). */
export const INVOICE_PAGE_PARAM = 'invoicePage';

/** How many invoices fill one page of the all-invoices list. */
export const INVOICE_PAGE_SIZE = 10;

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

/**
 * One invoice's status, worked out server-side from the payments recorded against it (PAID once
 * covered, PARTIAL once part paid, otherwise its stored SENT/DRAFT stage) — an invoice is no longer
 * flipped to paid by hand. Keyed UNDER the payments key: recording a payment invalidates
 * ['payments', invoiceId], which prefix-matches this key, so the status refreshes itself (CLAUDE.md
 * rule 5 — two features stay in step by sharing a key).
 */
export const invoiceStatusKey = (invoiceId: number) =>
  ['payments', invoiceId, 'status'] as const;

export function fetchInvoiceStatus(invoiceId: number): Promise<{ status: string }> {
  return getJson<{ status: string }>(`/api/invoices/${invoiceId}/status`);
}
