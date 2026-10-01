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

// How many invoices one page of the list shows. The server slices to this window; the pagination
// control steps through pages by the same count.
export const INVOICE_PAGE_SIZE = 10;

// A single lifecycle stage to narrow the list to ('' means every stage) and which PAGE of the
// narrowed list to fetch (1-based). The server reads the same `status` and `page` the UI holds, so
// the narrowing and the windowing are applied once, at the source, not re-done on the client. Both
// are appended to the query key below so each stage/page caches on its own, while a send/pay write
// still invalidates ['invoices'] by prefix and refreshes whichever window is on screen.
export const listAllInvoices = (status = '', page = 1): Promise<AllInvoice[]> =>
  getJson<AllInvoice[]>(
    `/api/invoices/all?status=${encodeURIComponent(status)}` +
      `&page=${page}&pageSize=${INVOICE_PAGE_SIZE}`,
  );
