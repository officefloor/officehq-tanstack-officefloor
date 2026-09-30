import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { InvoiceRowCells } from '../../slots/defs/invoiceRowCells';
import type { Invoice } from './InvoicesPanel';

// Cancel (void) an invoice sent by mistake — its own file, filling the invoice-row-cells slot so the
// panel is never edited to grow a new action. The cell queries for itself (['invoices'], the SAME
// key the panel owns) to read this invoice's status, and only offers the control while it is SENT
// (a DRAFT has not gone out; a PAID one is settled). Cancelling POSTs to /api/invoices/cancel and
// invalidates ['invoices'] so every view showing this invoice re-renders with VOID — never
// hand-maintained. The server flips it to VOID (so it stops counting toward what is owed) and writes
// the audit record. Carries data-testid="invoice-cancel-<id>" (the test contract).
export const contribution = InvoiceRowCells.fill({
  order: 40,
  Component: ({ invoiceId }: { invoiceId: number }) => {
    const queryClient = useQueryClient();
    const invoices = useQuery({
      queryKey: ['invoices'],
      queryFn: () => getJson<Invoice[]>('/api/invoices'),
    });
    const invoice = (invoices.data ?? []).find((i) => i.id === invoiceId);
    const cancel = useMutation({
      mutationFn: (id: number) => postJson<Invoice>('/api/invoices/cancel', { id }),
      onSuccess: () => {
        void queryClient.invalidateQueries({ queryKey: ['invoices'] });
      },
    });
    return (
      <td>
        {invoice?.status === 'SENT' && (
          <button
            type="button"
            data-testid={`invoice-cancel-${invoiceId}`}
            onClick={() => cancel.mutate(invoiceId)}
          >
            Cancel
          </button>
        )}
      </td>
    );
  },
});
