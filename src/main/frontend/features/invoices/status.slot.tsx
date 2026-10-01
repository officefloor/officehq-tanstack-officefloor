import { useQuery } from '@tanstack/react-query';
import { InvoiceDetail } from '../../slots/defs/invoiceDetail';
import { getInvoice } from './api';
import { paymentsKey } from './paymentsApi';

// This invoice's status, WORKED OUT from its payments — one panel filling the invoice.detail region.
// The server derives it (still owing / part paid / paid) from the payments recorded against the
// invoice, so there is no flipping it to paid by hand. It stays in step with the payments the same
// way every other panel does: by sharing the KEY. The query sits UNDER ['invoice-payments', id], so
// the record form's invalidate of that key (prefix match) refetches this status too — no import
// between them and nothing passed down.
function InvoiceStatus({ invoiceId }: { invoiceId: number }) {
  const { data: invoice } = useQuery({
    queryKey: [...paymentsKey(invoiceId), 'invoice'],
    queryFn: () => getInvoice(invoiceId),
  });

  if (!invoice) {
    return null;
  }

  return <p data-testid="invoice-status">{invoice.status}</p>;
}

export const contribution = InvoiceDetail.fill({ order: 5, Component: InvoiceStatus });
