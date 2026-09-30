import { useQuery } from '@tanstack/react-query';
import { InvoiceRow } from '../../slots/defs/invoiceRow';
import { invoicesKey, fetchInvoices } from './queries';

// An invoice's due date (when it is due) — its own file filling the invoice.row region (CLAUDE.md
// rule 3). Queries for itself under ['invoices', projectId] (rule 5) and reads its own row by id.
// Renders the literal ISO date under data-testid="invoice-due" (the test contract).
export const contribution = InvoiceRow.fill({
  order: 5,
  Component: ({ invoiceId, projectId }: { invoiceId: number; projectId: number; status: string }) => {
    const { data: invoices } = useQuery({
      queryKey: invoicesKey(projectId),
      queryFn: () => fetchInvoices(projectId),
    });
    const invoice = invoices?.find((i) => i.id === invoiceId);
    return <td data-testid="invoice-due">{invoice?.dueDate ?? ''}</td>;
  },
});
