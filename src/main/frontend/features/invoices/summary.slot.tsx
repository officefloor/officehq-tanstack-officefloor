import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { InvoiceDetail } from '../../slots/defs/invoiceDetail';
import { money } from '../../ui/money';

// An invoice's money summary as the server derives it: the subtotal (the sum of its line items), the
// percentage discount taken off it, the discount amount, and the final total (subtotal minus the
// discount). Derived server-side so the discount is worked out in one place.
type InvoiceSummary = {
  subtotal: number;
  discountPct: number;
  discount: number;
  total: number;
};

// The invoice's money summary on its detail page — its own file, filling the invoice-detail slot
// above the line items (order 5). It queries for itself under ['invoiceSummary', invoiceId]; the
// line items panel invalidates that key whenever a line changes, so the subtotal, discount and total
// stay in step without any import between them (rule 5). Carries the invoice-subtotal,
// invoice-discount and invoice-amount test anchors — the final total after the discount.
function InvoiceSummaryPanel({ invoiceId }: { invoiceId: number }) {
  const summary = useQuery({
    queryKey: ['invoiceSummary', invoiceId],
    queryFn: () => getJson<InvoiceSummary>(`/api/invoices/summary?invoiceId=${invoiceId}`),
  });

  const subtotal = Number(summary.data?.subtotal ?? 0);
  const discount = Number(summary.data?.discount ?? 0);
  const total = Number(summary.data?.total ?? 0);

  return (
    <section data-testid="invoice-summary">
      <p>
        <span>Subtotal</span>
        <span data-testid="invoice-subtotal">{money(subtotal)}</span>
      </p>
      <p>
        <span>Discount</span>
        <span data-testid="invoice-discount">{money(discount)}</span>
      </p>
      <p>
        <span>Total</span>
        <span data-testid="invoice-amount">{money(total)}</span>
      </p>
    </section>
  );
}

export const contribution = InvoiceDetail.fill({
  order: 5,
  Component: InvoiceSummaryPanel,
});
