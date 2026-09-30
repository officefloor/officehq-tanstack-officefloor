import { useMutation, useQueryClient } from '@tanstack/react-query';
import { InvoiceRow } from '../../slots/defs/invoiceRow';
import { invoicesKey, sendInvoice } from './queries';

// The "send this invoice" action on every invoice row — its own file filling the invoice.row region
// (CLAUDE.md rule 3). Only a DRAFT invoice can be sent, so the button renders only while the row's
// status (from context) is DRAFT; once sent it gives way to the pay action. The write is a mutation
// that invalidates ['invoices', projectId] so the list (and its status cell) refresh themselves
// (rule 5); the audited record is written server-side. Carries data-testid="invoice-send-<id>".
export const contribution = InvoiceRow.fill({
  order: 15,
  Component: ({ invoiceId, projectId, status }: { invoiceId: number; projectId: number; status: string }) => {
    const queryClient = useQueryClient();
    const mutation = useMutation({
      mutationFn: () => sendInvoice(invoiceId),
      onSuccess: () => {
        void queryClient.invalidateQueries({ queryKey: invoicesKey(projectId) });
      },
    });
    if (status !== 'DRAFT') {
      return null;
    }
    return (
      <td>
        <button
          data-testid={`invoice-send-${invoiceId}`}
          type="button"
          onClick={() => mutation.mutate()}
        >
          Send
        </button>
      </td>
    );
  },
});
