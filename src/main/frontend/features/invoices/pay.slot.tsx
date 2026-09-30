import { useMutation, useQueryClient } from '@tanstack/react-query';
import { InvoiceRow } from '../../slots/defs/invoiceRow';
import { invoicesKey, payInvoice } from './queries';

// The "mark this invoice paid" action on every invoice row — its own file filling the invoice.row
// region (CLAUDE.md rule 3). The write is a mutation that invalidates ['invoices', projectId] so the
// list (and its status cell) refresh themselves (rule 5); the audited record is written server-side.
// Carries data-testid="invoice-pay-<id>" (the test contract).
export const contribution = InvoiceRow.fill({
  order: 20,
  Component: ({ invoiceId, projectId }: { invoiceId: number; projectId: number; status: string }) => {
    const queryClient = useQueryClient();
    const mutation = useMutation({
      mutationFn: () => payInvoice(invoiceId),
      onSuccess: () => {
        void queryClient.invalidateQueries({ queryKey: invoicesKey(projectId) });
      },
    });
    return (
      <td>
        <button
          data-testid={`invoice-pay-${invoiceId}`}
          type="button"
          onClick={() => mutation.mutate()}
        >
          Mark paid
        </button>
      </td>
    );
  },
});
