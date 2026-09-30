import { useQuery } from '@tanstack/react-query';
import { InvoiceDetail } from '../../slots/defs/invoiceDetail';
import { fetchInvoiceDiscount, invoiceDiscountKey } from './queries';
import { formatMoney } from '../../ui/money';

// The discount breakdown on an invoice's detail page — its own file filling the InvoiceDetail region
// (CLAUDE.md rule 3); nothing existing is edited to add it. The subtotal (sum of the lines) and the
// percentage discount taken off are DERIVED server-side, so they always agree with the line items
// and the final total (which the line-items table already surfaces under data-testid
// "invoice-amount"). It queries for itself under the shared ['lineitems', invoiceId] key, so editing
// a line (which invalidates that key) refreshes the breakdown for free (rule 5). Renders under the
// data-testids the test contract expects.
function InvoiceDiscountPanel({ invoiceId }: { invoiceId: number }) {
  const { data } = useQuery({
    queryKey: invoiceDiscountKey(invoiceId),
    queryFn: () => fetchInvoiceDiscount(invoiceId),
  });

  if (!data) {
    return null;
  }

  return (
    <section data-testid="invoice-discount-panel">
      <dl>
        <dt>Subtotal</dt>
        <dd data-testid="invoice-subtotal">{formatMoney(data.subtotal)}</dd>
        <dt>Discount</dt>
        <dd data-testid="invoice-discount">{formatMoney(data.discount)}</dd>
      </dl>
    </section>
  );
}

export const contribution = InvoiceDetail.fill({ order: 10, Component: InvoiceDiscountPanel });
