import { useQuery } from '@tanstack/react-query';
import { InvoiceRow } from '../../slots/defs/invoiceRow';
import { invoicesKey, fetchInvoices, type Invoice } from './queries';
import { formatMoney } from '../../ui/money';

// The "still due after payments" cell on every invoice row — its own file filling the invoice.row
// region (CLAUDE.md rule 3). The amount due (invoice amount minus its payments) is derived
// server-side and carried on each invoice, so this cell reads the SAME ['invoices', projectId]
// query the table already loaded (rule 5) and finds its own invoice — no parent hands it the value,
// and a payment write that invalidates the key refreshes this cell for free. Renders under
// data-testid="invoice-due-amount" (the test contract).
export const contribution = InvoiceRow.fill({
  order: 15,
  Component: ({ invoiceId, projectId }: { invoiceId: number; projectId: number; status: string }) => {
    const { data: invoices } = useQuery({
      queryKey: invoicesKey(projectId),
      queryFn: () => fetchInvoices(projectId),
    });
    const invoice = invoices?.find((row: Invoice) => row.id === invoiceId);
    if (!invoice) {
      return null;
    }
    return <td data-testid="invoice-due-amount">{formatMoney(invoice.due)}</td>;
  },
});
