import { InvoiceRow } from '../../slots/defs/invoiceRow';

// An invoice's payment status cell — its own file filling the invoice.row region (CLAUDE.md rule 3).
// Reads the status from the row's context (the value the table already queried for) and renders it
// under data-testid="invoice-status" (the test contract).
export const contribution = InvoiceRow.fill({
  order: 10,
  Component: ({ status }: { invoiceId: number; projectId: number; status: string }) => (
    <td data-testid="invoice-status">{status}</td>
  ),
});
