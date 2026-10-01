import { useMutation, useQueryClient } from '@tanstack/react-query';
import { InvoiceRow } from '../../slots/defs/invoiceRow';
import { invoicesKey, payInvoice } from './api';

// Mark an invoice paid — one new file filling the per-invoice-row action slot. The click is a
// useMutation (never a hand-rolled status flip in state); on success we invalidate the project's
// ['invoices', projectId] key so the invoices panel refetches the new status. The server also
// appends the audit record, so "keep a record every time" is handled where the side-effect lives.
function PayInvoice({ invoiceId, projectId }: { invoiceId: number; projectId: number }) {
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn: () => payInvoice(invoiceId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: invoicesKey(projectId) });
    },
  });

  return (
    <button
      data-testid={`invoice-pay-${invoiceId}`}
      type="button"
      disabled={mutation.isPending}
      onClick={() => mutation.mutate()}
    >
      Mark paid
    </button>
  );
}

export const contribution = InvoiceRow.fill({ order: 0, Component: PayInvoice });
