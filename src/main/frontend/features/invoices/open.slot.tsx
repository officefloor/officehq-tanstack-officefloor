import { Link } from '@tanstack/react-router';
import { InvoiceRow } from '../../slots/defs/invoiceRow';

// The "open this invoice" action on every invoice row — its own file filling the invoice.row region
// (CLAUDE.md rule 3). Drilling in is a route (rule 2): the link navigates to /invoice/$invoiceId,
// where the invoice's line items live. Carries data-testid="invoice-open-<id>" (the test contract).
export const contribution = InvoiceRow.fill({
  order: 0,
  Component: ({ invoiceId }: { invoiceId: number; projectId: number; status: string }) => (
    <td>
      <Link
        to="/invoice/$invoiceId"
        params={{ invoiceId: String(invoiceId) }}
        data-testid={`invoice-open-${invoiceId}`}
      >
        Open
      </Link>
    </td>
  ),
});
