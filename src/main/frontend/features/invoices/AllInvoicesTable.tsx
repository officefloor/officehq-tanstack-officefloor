import { useQuery } from '@tanstack/react-query';
import {
  allInvoicesKey,
  fetchAllInvoices,
  INVOICE_STATUS_PARAM,
  INVOICE_PAGE_PARAM,
  INVOICE_PAGE_SIZE,
  type AllInvoice,
} from './queries';
import { formatMoney } from '../../ui/money';
import { AllInvoicesToolbar } from '../../slots/defs/allInvoicesToolbar';
import { useSearchParam, asString, asNumber } from '../../url/useSearchParam';

// The one place listing every invoice from every project. Queries for itself under ['invoices',
// 'all'] — never handed its data by a parent (CLAUDE.md rule 5). Each row shows the invoice's
// project NAME (from the server-side join) and its lifecycle stage. The stage filter lives in the
// URL (rule 4): the list reads `invoiceStatus` and, when set, drops rows at any other stage.
export function AllInvoicesTable() {
  const { data: invoices } = useQuery({ queryKey: allInvoicesKey, queryFn: fetchAllInvoices });
  const [status] = useSearchParam(INVOICE_STATUS_PARAM, asString);
  const [rawPage] = useSearchParam(INVOICE_PAGE_PARAM, asNumber);

  if (!invoices) {
    return null;
  }

  if (invoices.length === 0) {
    return <p data-testid="all-invoices-empty">No invoices yet.</p>;
  }

  const matching = status ? invoices.filter((invoice) => invoice.status === status) : invoices;
  // A page at a time (CLAUDE.md rule 4): the page number lives in the URL, owned by the pagination
  // control; this list reads the same key and slices to that window. Clamp to a valid page so a
  // stale/over-range number (e.g. after the stage filter shrinks the set) still shows real rows.
  const totalPages = Math.max(1, Math.ceil(matching.length / INVOICE_PAGE_SIZE));
  const page = Math.min(Math.max(rawPage ?? 1, 1), totalPages);
  const shown = matching.slice((page - 1) * INVOICE_PAGE_SIZE, page * INVOICE_PAGE_SIZE);

  return (
    <>
      <AllInvoicesToolbar.Slot />
      <table data-testid="all-invoices-table">
      <thead>
        <tr>
          <th>Job</th>
          <th>Amount</th>
          <th>Stage</th>
        </tr>
      </thead>
      <tbody>
        {shown.map((invoice: AllInvoice) => (
          <tr key={invoice.id} data-testid={`invoice-row-${invoice.id}`}>
            <td data-testid="invoice-project">{invoice.projectName}</td>
            <td data-testid="invoice-amount">{formatMoney(invoice.amount)}</td>
            <td data-testid="invoice-status">{invoice.status}</td>
          </tr>
        ))}
      </tbody>
    </table>
    </>
  );
}
