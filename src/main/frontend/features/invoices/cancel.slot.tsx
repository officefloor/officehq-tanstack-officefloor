import { InvoiceRow } from '../../slots/defs/invoiceRow';
import type { Invoice } from './invoices';
import { useVoidInvoice } from './voidInvoice';

// "Cancel this invoice" — a self-contained row control filling the invoice-row region. The table is
// not edited to add it beyond rendering the region (CLAUDE.md rule 3). It offers to void an invoice
// that was sent by mistake: only a SENT invoice can be cancelled, so the control shows only then.
// Sharing the ['invoices'] query key (via useVoidInvoice), the list re-reads on its own once the
// invoice is voided — the row then reads VOID and the invoice stops counting toward what is owed.
function CancelInvoice({ invoice }: { invoice: Invoice }) {
  const voidInvoice = useVoidInvoice();
  if (invoice.status !== 'SENT') {
    return null;
  }
  return (
    <button
      type="button"
      data-testid={`invoice-cancel-${invoice.id}`}
      disabled={voidInvoice.isPending}
      onClick={() => voidInvoice.mutate({ id: invoice.id })}
    >
      Cancel
    </button>
  );
}

export const contribution = InvoiceRow.fill({ order: 20, Component: CancelInvoice });
