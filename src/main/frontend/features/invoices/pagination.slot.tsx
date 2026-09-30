import { useQuery } from '@tanstack/react-query';
import { AllInvoicesToolbar } from '../../slots/defs/allInvoicesToolbar';
import { useSearchParam, asString, asNumber } from '../../url/useSearchParam';
import {
  allInvoicesKey,
  fetchAllInvoices,
  INVOICE_STATUS_PARAM,
  INVOICE_PAGE_PARAM,
  INVOICE_PAGE_SIZE,
  type AllInvoice,
} from './queries';

// The "one page at a time" control — its own file filling the all-invoices toolbar region
// (CLAUDE.md rule 3). It owns the `invoicePage` URL key (rule 4): the current page outlives a click,
// so it lives in the URL, and the list (AllInvoicesTable) reads the very same key to slice its rows.
// No callback, no shared state — just the key. It queries the invoices for itself (rule 5) purely to
// know how many pages there are, so it can disable next/prev at the ends; it reads the same
// `invoiceStatus` key the list narrows by, so the page count matches what the list shows.
function InvoicePagination() {
  const { data: invoices } = useQuery({ queryKey: allInvoicesKey, queryFn: fetchAllInvoices });
  const [status] = useSearchParam(INVOICE_STATUS_PARAM, asString);
  const [rawPage, setPage] = useSearchParam(INVOICE_PAGE_PARAM, asNumber);

  const matching = (invoices ?? []).filter(
    (invoice: AllInvoice) => !status || invoice.status === status,
  );
  const totalPages = Math.max(1, Math.ceil(matching.length / INVOICE_PAGE_SIZE));
  const page = Math.min(Math.max(rawPage ?? 1, 1), totalPages);

  return (
    <div data-testid="invoice-pagination">
      <button
        type="button"
        data-testid="invoice-page-prev"
        disabled={page <= 1}
        onClick={() => setPage(page - 1)}
      >
        Previous
      </button>
      <span data-testid="invoice-page-label">{page}</span>
      <button
        type="button"
        data-testid="invoice-page-next"
        disabled={page >= totalPages}
        onClick={() => setPage(page + 1)}
      >
        Next
      </button>
    </div>
  );
}

export const contribution = AllInvoicesToolbar.fill({ order: 20, Component: InvoicePagination });
