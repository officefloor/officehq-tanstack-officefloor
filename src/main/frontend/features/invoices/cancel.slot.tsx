import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { InvoiceRow } from '../../slots/defs/invoiceRow';
import { dashboardKey } from '../dashboard/api';
import { cancelInvoice, invoicesKey, listInvoices, type Invoice } from './api';

// Cancel an invoice that was sent by mistake — one new file filling the per-invoice-row action slot.
// The button only appears while the invoice is SENT (a sent-by-mistake invoice): the slot queries
// ['invoices', projectId] for its OWN row's status rather than being handed it, so it stays in step
// through the shared key. The click is a useMutation; on success we invalidate that key so the row's
// status and the sibling actions refetch, and ['dashboard'] so the outstanding total drops the voided
// amount. The server appends the audit record and stops the invoice counting towards what is owed, so
// "note it / stop counting it" is handled where the side-effect lives.
function CancelInvoice({ invoiceId, projectId }: { invoiceId: number; projectId: number }) {
  const queryClient = useQueryClient();
  const { data: invoices } = useQuery({
    queryKey: invoicesKey(projectId),
    queryFn: () => listInvoices(projectId),
  });
  const mutation = useMutation({
    mutationFn: () => cancelInvoice(invoiceId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: invoicesKey(projectId) });
      void queryClient.invalidateQueries({ queryKey: dashboardKey });
    },
  });

  const invoice = invoices?.find((i: Invoice) => i.id === invoiceId);
  if (!invoice || invoice.status !== 'SENT') {
    return null;
  }

  return (
    <button
      data-testid={`invoice-cancel-${invoiceId}`}
      type="button"
      disabled={mutation.isPending}
      onClick={() => mutation.mutate()}
    >
      Cancel invoice
    </button>
  );
}

export const contribution = InvoiceRow.fill({ order: 20, Component: CancelInvoice });
