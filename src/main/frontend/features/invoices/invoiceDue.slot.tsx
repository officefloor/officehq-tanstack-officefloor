import { useQuery } from '@tanstack/react-query';
import { InvoiceRow } from '../../slots/defs/invoiceRow';
import { invoiceDueKey, getInvoiceDue } from './dueApi';
import { formatMoney } from '../../ui/money';

// How much is still left to pay on this invoice after any payments — one new file filling the
// per-invoice-row action slot, so the cell lives inside its own invoice-row-<id>. The balance is
// read from the server under ['invoice-due', invoiceId] (never computed in state and never handed
// down by the table): the slot queries for its OWN row. Amounts render with two decimals.
function InvoiceDueAmount({ invoiceId }: { invoiceId: number; projectId: number }) {
  const { data: due } = useQuery({
    queryKey: invoiceDueKey(invoiceId),
    queryFn: () => getInvoiceDue(invoiceId),
  });

  if (!due) {
    return null;
  }

  return <span data-testid="invoice-due-amount">{formatMoney(due.amountDue)}</span>;
}

export const contribution = InvoiceRow.fill({ order: 10, Component: InvoiceDueAmount });
