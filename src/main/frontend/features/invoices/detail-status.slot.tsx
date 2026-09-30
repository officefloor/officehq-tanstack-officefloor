import { useQuery } from '@tanstack/react-query';
import { InvoiceDetail } from '../../slots/defs/invoiceDetail';
import { fetchInvoiceStatus, invoiceStatusKey } from './queries';

// The invoice's status on its detail page — its own file filling the InvoiceDetail region (CLAUDE.md
// rule 3); nothing existing is edited to add it. The status is DERIVED from the payments recorded
// (PAID / PARTIAL / SENT), so it is never flipped by hand. It queries for itself under the shared
// payments key, so recording a payment (which invalidates ['payments', invoiceId]) refreshes it
// (rule 5). Renders under data-testid="invoice-status" (the test contract).
function InvoiceStatusPanel({ invoiceId }: { invoiceId: number }) {
  const { data } = useQuery({
    queryKey: invoiceStatusKey(invoiceId),
    queryFn: () => fetchInvoiceStatus(invoiceId),
  });

  if (!data) {
    return null;
  }

  return <p data-testid="invoice-status">{data.status}</p>;
}

export const contribution = InvoiceDetail.fill({ order: 5, Component: InvoiceStatusPanel });
