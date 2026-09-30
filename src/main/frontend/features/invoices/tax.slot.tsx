import { useQuery } from '@tanstack/react-query';
import { InvoiceDetail } from '../../slots/defs/invoiceDetail';
import { fetchInvoiceDiscount, invoiceDiscountKey } from './queries';
import { formatMoney } from '../../ui/money';

// The sales-tax line on an invoice's detail page — its own file filling the InvoiceDetail region
// (CLAUDE.md rule 3); nothing existing is edited to add it. The tax is DERIVED server-side, worked
// out AFTER the discount (applied to the discounted subtotal), so it always agrees with the subtotal,
// the discount and the final total (which the line-items table surfaces under data-testid
// "invoice-amount"). It queries for itself under the shared ['lineitems', invoiceId] key, so editing
// a line (which invalidates that key) refreshes the tax for free (rule 5). Renders under the
// data-testid the test contract expects; ordered just after the discount panel (order 10).
function InvoiceTaxPanel({ invoiceId }: { invoiceId: number }) {
  const { data } = useQuery({
    queryKey: invoiceDiscountKey(invoiceId),
    queryFn: () => fetchInvoiceDiscount(invoiceId),
  });

  if (!data) {
    return null;
  }

  return (
    <section data-testid="invoice-tax-panel">
      <dl>
        <dt>Tax</dt>
        <dd data-testid="invoice-tax">{formatMoney(data.tax)}</dd>
      </dl>
    </section>
  );
}

export const contribution = InvoiceDetail.fill({ order: 20, Component: InvoiceTaxPanel });
