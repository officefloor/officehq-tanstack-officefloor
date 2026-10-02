import { InvoiceDetail } from '../../slots/defs/invoiceDetail';
import { formatMoney } from '../../ui/money';
import { useInvoiceSummary } from './invoiceSummary';

// The money breakdown panel on the invoice detail page — its own file, filling the shared
// invoice.detail region (CLAUDE.md rule 3). It reads the server-worked-out summary for its own
// invoice and shows the subtotal (the sum of the lines), the discount taken off it, and the final
// total. Sharing the ['invoices'] query key, it stays in step with the line items: adding a line
// re-reads it. The final total is the invoice's amount, shown under the line items table as
// `invoice-amount`; this panel surfaces the two figures that make it up.
function InvoiceSummary({ invoiceId }: { invoiceId: number }) {
  const { data: summary } = useInvoiceSummary(invoiceId);

  if (!summary) {
    return null;
  }

  return (
    <section data-testid="invoice-summary">
      <h2>Summary</h2>
      <dl>
        <dt>Subtotal</dt>
        <dd data-testid="invoice-subtotal">{formatMoney(summary.subtotal)}</dd>
        <dt>Discount ({Number(summary.discountPct)}%)</dt>
        <dd data-testid="invoice-discount">{formatMoney(summary.discount)}</dd>
        <dt>Tax ({Number(summary.taxPct)}%)</dt>
        <dd data-testid="invoice-tax">{formatMoney(summary.tax)}</dd>
        <dt>Total</dt>
        <dd data-testid="invoice-total">{formatMoney(summary.total)}</dd>
      </dl>
    </section>
  );
}

export const contribution = InvoiceDetail.fill({ order: 20, Component: InvoiceSummary });
