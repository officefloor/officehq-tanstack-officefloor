import { AllInvoicesToolbar } from '../../slots/defs/allInvoicesToolbar';
import { useSearchParam, asNumber } from '../../url/useSearchParam';

// Step through the all-invoices list a page at a time — one new *.slot.tsx filling the
// invoices.toolbar region. It owns the `invoicePage` URL key; the list reads the same key and asks
// the server for just that window. Nothing is passed between them: they stay in step through the
// shared search param. Prev floors at the first page; the label shows the page the list is on.
function InvoicePagination() {
  const [page, setPage] = useSearchParam('invoicePage', asNumber);
  const current = page ?? 1;

  return (
    <span>
      <button
        type="button"
        data-testid="invoice-page-prev"
        disabled={current <= 1}
        onClick={() => setPage(Math.max(1, current - 1))}
      >
        Previous
      </button>
      <span data-testid="invoice-page-label">{current}</span>
      <button
        type="button"
        data-testid="invoice-page-next"
        onClick={() => setPage(current + 1)}
      >
        Next
      </button>
    </span>
  );
}

export const contribution = AllInvoicesToolbar.fill({ order: 20, Component: InvoicePagination });
