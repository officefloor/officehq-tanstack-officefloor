import { useQuery } from '@tanstack/react-query';
import { InvoiceRow } from '../../slots/defs/invoiceRow';
import { invoicesKey, fetchInvoices } from './queries';

// An invoice's issued date (when it went out) — its own file filling the invoice.row region
// (CLAUDE.md rule 3). Queries for itself under ['invoices', projectId] (rule 5) and reads its own
// row by id, rather than being handed the value by the table. Renders the literal ISO date under
// data-testid="invoice-issued" (the test contract).
export const contribution = InvoiceRow.fill({
  order: 4,
  Component: ({ invoiceId, projectId }: { invoiceId: number; projectId: number; status: string }) => {
    const { data: invoices } = useQuery({
      queryKey: invoicesKey(projectId),
      queryFn: () => fetchInvoices(projectId),
    });
    const invoice = invoices?.find((i) => i.id === invoiceId);
    return <td data-testid="invoice-issued">{invoice?.issuedDate ?? ''}</td>;
  },
});
