import { useMutation, useQueryClient } from '@tanstack/react-query';
import { InvoiceRow } from '../../slots/defs/invoiceRow';
import { invoicesKey, voidInvoice } from './queries';

// The "cancel this invoice" action on every invoice row — its own file filling the invoice.row
// region (CLAUDE.md rule 3). Only a SENT invoice can be cancelled (an invoice sent by mistake), so
// the button renders only while the row's status (from context) is SENT; once voided it disappears.
// The write is a mutation that invalidates ['invoices', projectId] so the list (and its status cell)
// refresh themselves (rule 5); the audited record is written server-side, and a VOID invoice stops
// counting toward what is owed. Carries data-testid="invoice-cancel-<id>".
export const contribution = InvoiceRow.fill({
  order: 20,
  Component: ({ invoiceId, projectId, status }: { invoiceId: number; projectId: number; status: string }) => {
    const queryClient = useQueryClient();
    const mutation = useMutation({
      mutationFn: () => voidInvoice(invoiceId),
      onSuccess: () => {
        void queryClient.invalidateQueries({ queryKey: invoicesKey(projectId) });
      },
    });
    if (status !== 'SENT') {
      return null;
    }
    return (
      <td>
        <button
          data-testid={`invoice-cancel-${invoiceId}`}
          type="button"
          onClick={() => mutation.mutate()}
        >
          Cancel
        </button>
      </td>
    );
  },
});
