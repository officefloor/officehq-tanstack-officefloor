import { useQuery } from '@tanstack/react-query';
import { InvoiceDetail } from '../../slots/defs/invoiceDetail';
import { getInvoiceSummary, invoiceSummaryKey } from './summaryApi';
import { formatMoney } from '../../ui/money';

// An invoice's money summary — one panel filling the invoice.detail region: the SUBTOTAL (what its
// line items add up to), the DISCOUNT taken off it (a percentage of the subtotal), the sales TAX
// added on top (a percentage of the discounted amount, applied after the discount), and the final
// TOTAL (subtotal minus the discount, plus the tax on what is left). Server data is read under the
// summary key (never copied into state); the server derives the figures so there is no float drift,
// and the key shares the line items' prefix so a line add/edit/remove refreshes these figures too.
// This panel owns the invoice-amount (the final total) — the line items table above is just the
// editable lines. Money renders with two decimals.
function InvoiceSummary({ invoiceId }: { invoiceId: number }) {
  const { data: summary } = useQuery({
    queryKey: invoiceSummaryKey(invoiceId),
    queryFn: () => getInvoiceSummary(invoiceId),
  });

  if (!summary) {
    return null;
  }

  return (
    <dl data-testid="invoice-summary">
      <dt>Subtotal</dt>
      <dd data-testid="invoice-subtotal">{formatMoney(summary.subtotal)}</dd>
      <dt>Discount</dt>
      <dd data-testid="invoice-discount">{formatMoney(summary.discount)}</dd>
      <dt>Tax</dt>
      <dd data-testid="invoice-tax">{formatMoney(summary.tax)}</dd>
      <dt>Total</dt>
      <dd data-testid="invoice-amount">{formatMoney(summary.total)}</dd>
    </dl>
  );
}

export const contribution = InvoiceDetail.fill({ order: 20, Component: InvoiceSummary });
