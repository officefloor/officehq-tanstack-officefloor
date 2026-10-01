import { getJson } from '../../api/http';

// Every invoice across every project, as the API exposes it for the "all invoices" page. Each row
// carries its project's NAME (the cross-entity join done on the server) and its lifecycle stage
// (status), so the one list shows which project each invoice is for and what stage it is at without
// a second lookup. The query key ['invoices', 'all'] is its own handle — a send/pay write invalidates
// ['invoices'] by prefix, so this list refreshes in step with no import between them.
export type AllInvoice = {
  id: number;
  projectId: number;
  projectName: string;
  status: string;
  amount: number;
};

export const allInvoicesKey = ['invoices', 'all'] as const;

// A single lifecycle stage to narrow the list to ('' means every stage). The server reads the same
// `status` key, so the narrowing is applied once, at the source, not re-filtered on the client. It
// is appended to the query key below so each stage caches on its own, while a send/pay write still
// invalidates ['invoices'] by prefix and refreshes whichever stage is on screen.
export const listAllInvoices = (status = ''): Promise<AllInvoice[]> =>
  getJson<AllInvoice[]>(`/api/invoices/all?status=${encodeURIComponent(status)}`);
