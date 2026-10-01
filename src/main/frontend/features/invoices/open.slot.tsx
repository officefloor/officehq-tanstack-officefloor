import { Link } from '@tanstack/react-router';
import { InvoiceRow } from '../../slots/defs/invoiceRow';

// The link that opens an invoice's detail page — one new file filling the per-invoice-row action
// slot. Drilling into an invoice is a child route (routes/invoice.$invoiceId.tsx), never a flag, so
// this is a plain navigation to that route. Its testid is invoice-open-<id> (the test contract).
export const contribution = InvoiceRow.fill({
  order: 10,
  Component: ({ invoiceId }: { invoiceId: number }) => (
    <Link
      to="/invoice/$invoiceId"
      params={{ invoiceId: String(invoiceId) }}
      data-testid={`invoice-open-${invoiceId}`}
    >
      Open
    </Link>
  ),
});
