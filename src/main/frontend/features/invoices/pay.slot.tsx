import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { InvoiceRow } from '../../slots/defs/invoiceRow';
import { invoicesKey, listInvoices, payInvoice, type Invoice } from './api';

// Mark an invoice paid — one new file filling the per-invoice-row action slot. Payment is the last
// step of the DRAFT -> SENT -> PAID lifecycle, so the button only appears once the invoice has been
// SENT: the slot queries ['invoices', projectId] for its OWN row's status rather than being handed
// it. The click is a useMutation (never a hand-rolled status flip in state); on success we invalidate
// the project's ['invoices', projectId] key so the invoices panel refetches the new status. The
// server also appends the audit record, so "keep a record every time" is handled where the
// side-effect lives.
function PayInvoice({ invoiceId, projectId }: { invoiceId: number; projectId: number }) {
  const queryClient = useQueryClient();
  const { data: invoices } = useQuery({
    queryKey: invoicesKey(projectId),
    queryFn: () => listInvoices(projectId),
  });
  const mutation = useMutation({
    mutationFn: () => payInvoice(invoiceId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: invoicesKey(projectId) });
    },
  });

  const invoice = invoices?.find((i: Invoice) => i.id === invoiceId);
  if (!invoice || invoice.status !== 'SENT') {
    return null;
  }

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
