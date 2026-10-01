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

export const listAllInvoices = (): Promise<AllInvoice[]> =>
  getJson<AllInvoice[]>('/api/invoices/all');
