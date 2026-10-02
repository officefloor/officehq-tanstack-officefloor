import { AllInvoicesToolbar } from '../../slots/defs/allInvoicesToolbar';
import { useSearchParam, asString } from '../../url/useSearchParam';
import { useAllInvoices } from './allInvoices';
import { INVOICE_PAGE_SIZE, useInvoicePage } from './pagination';

// "Next / Previous" — a self-contained control that owns the `invoicePage` URL key. The all-invoices
// list reads the same key and shows only that page's rows. The pager queries the invoices itself (and
// reads the shared `invoiceStatus` filter) so it knows how many pages there are and can stop at the
// ends — it shares the query key and URL keys with the list, never an import of it.
function InvoicePager() {
  const [page, setPage] = useInvoicePage();
  const { data: invoices } = useAllInvoices();
  const [status] = useSearchParam('invoiceStatus', asString);

  const shown = invoices
    ? status
      ? invoices.filter((invoice) => invoice.status === status)
      : invoices
    : [];
  const lastPage = Math.max(1, Math.ceil(shown.length / INVOICE_PAGE_SIZE));

  return (
    <span data-testid="invoice-pager">
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
        disabled={page >= lastPage}
        onClick={() => setPage(page + 1)}
      >
        Next
      </button>
    </span>
  );
}

export const contribution = AllInvoicesToolbar.fill({ order: 20, Component: InvoicePager });
