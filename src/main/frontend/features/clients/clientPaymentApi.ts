import { postJson } from '../../api/http';

// Recording a lump payment a client made and splitting it across several of their open invoices.
// The body names the client, the date it was paid, the total amount, and how that total is
// allocated across invoices (one slice per invoice). Each slice lands as its own payment row on the
// server, so each invoice's balance then reflects its share. There is no new query key here — a
// successful split invalidates the SHARED keys the client statement, the project invoices and each
// invoice's balance already read (['clients'], ['invoices'], ['invoice-due'], ...), so everything
// showing those figures refetches with no import between the panels.
export type PaymentAllocation = {
  invoiceId: number;
  amount: number;
};

export type NewClientPayment = {
  clientId: number;
  date: string;
  amount: number;
  allocations: PaymentAllocation[];
};

export type RecordedPayment = {
  id: number;
  invoiceId: number;
  amount: number;
  date: string;
};

export const createClientPayment = (body: NewClientPayment): Promise<RecordedPayment[]> =>
  postJson<RecordedPayment[]>('/api/clients/payments', body);
