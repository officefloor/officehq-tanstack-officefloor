import { getJson } from '../../api/http';

// A client's statement as the API exposes it: all of the client's invoices in one place, each with
// how much is still due, and the total still owed (the sum of those dues). The query key is scoped
// to the client, ['clients', clientId, 'statement'], so each client's statement reads only its own
// data; sharing the ['clients'] prefix means a client write (or an invoice/payment change
// invalidating by this key) keeps it in step with the rest of the client page.
export type StatementInvoice = {
  id: number;
  projectId: number;
  status: string;
  amount: number;
  amountDue: number;
};

export type StatementProject = {
  projectId: number;
  name: string;
  subtotal: number;
  invoices: StatementInvoice[];
};

export type ClientStatement = {
  clientId: number;
  invoices: StatementInvoice[];
  projects: StatementProject[];
  outstandingTotal: number;
  // The currency this client is billed in — every figure on the statement is shown in it.
  currency: string;
};

export const clientStatementKey = (clientId: number) =>
  ['clients', clientId, 'statement'] as const;

export const getClientStatement = (clientId: number): Promise<ClientStatement> =>
  getJson<ClientStatement>(`/api/clients/statement?clientId=${clientId}`);
