import { useMutation, useQueryClient } from '@tanstack/react-query';
import { InvoiceRow } from '../../slots/defs/invoiceRow';
import { invoicesKey, payInvoice } from './queries';

// The "mark this invoice paid" action on every invoice row — its own file filling the invoice.row
// region (CLAUDE.md rule 3). Payment is only allowed once the invoice has been SENT, so the button
// renders only while the row's status (from context) is SENT; a DRAFT row shows nothing here. The
// write is a mutation that invalidates ['invoices', projectId] so the list (and its status cell)
// refresh themselves (rule 5); the audited record is written server-side. Carries
// data-testid="invoice-pay-<id>" (the test contract).
export const contribution = InvoiceRow.fill({
  order: 20,
  Component: ({ invoiceId, projectId, status }: { invoiceId: number; projectId: number; status: string }) => {
    const queryClient = useQueryClient();
    const mutation = useMutation({
      mutationFn: () => payInvoice(invoiceId),
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
