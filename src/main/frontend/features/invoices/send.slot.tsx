import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { InvoiceRow } from '../../slots/defs/invoiceRow';
import { invoicesKey, listInvoices, sendInvoice, type Invoice } from './api';

// Send a draft invoice — one new file filling the per-invoice-row action slot. The button only
// appears while the invoice is a DRAFT (the first step of the DRAFT -> SENT -> PAID lifecycle): the
// slot queries ['invoices', projectId] for its OWN row's status rather than being handed it, so it
// stays in step through the shared key. The click is a useMutation; on success we invalidate that
// key so the row's status and the pay control refetch. The server appends the audit record, so
// "keep a record when I send one" is handled where the side-effect lives.
function SendInvoice({ invoiceId, projectId }: { invoiceId: number; projectId: number }) {
  const queryClient = useQueryClient();
  const { data: invoices } = useQuery({
    queryKey: invoicesKey(projectId),
    queryFn: () => listInvoices(projectId),
  });
  const mutation = useMutation({
    mutationFn: () => sendInvoice(invoiceId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: invoicesKey(projectId) });
    },
  });

  const invoice = invoices?.find((i: Invoice) => i.id === invoiceId);
  if (!invoice || invoice.status !== 'DRAFT') {
    return null;
  }

  return (
    <button
      data-testid={`invoice-send-${invoiceId}`}
      type="button"
      disabled={mutation.isPending}
      onClick={() => mutation.mutate()}
    >
      Send invoice
    </button>
  );
}

export const contribution = InvoiceRow.fill({ order: 10, Component: SendInvoice });
