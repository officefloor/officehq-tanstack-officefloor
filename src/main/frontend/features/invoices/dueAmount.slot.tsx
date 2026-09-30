import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { InvoiceRowCells } from '../../slots/defs/invoiceRowCells';
import { money } from '../../ui/money';
import type { Invoice } from './InvoicesPanel';

// The "amount still due" cell of an invoice row — its own file, filling the invoice-row-cells slot.
// The panel is not edited to know how the due amount is worked out: this cell queries for itself
// (['invoices'], the SAME key the panel owns) and reads the invoice's server-derived amountDue (its
// amount minus every payment recorded against it), so it stays in step whenever a payment changes
// and the list is invalidated. Carries data-testid="invoice-due-amount" (the test contract).
export const contribution = InvoiceRowCells.fill({
  order: 10,
  Component: ({ invoiceId }: { invoiceId: number }) => {
    const invoices = useQuery({
      queryKey: ['invoices'],
      queryFn: () => getJson<Invoice[]>('/api/invoices'),
    });
    const invoice = (invoices.data ?? []).find((i) => i.id === invoiceId);
    const due = invoice ? Number(invoice.amountDue) : 0;
    return <td data-testid="invoice-due-amount">{money(due, invoice?.currency)}</td>;
  },
});
