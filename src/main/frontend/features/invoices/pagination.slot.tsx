import { AllInvoicesToolbar } from '../../slots/defs/allInvoicesToolbar';
import { asNumber, useSearchParam } from '../../url/useSearchParam';

// The page-through controls for the all-invoices list — its own file, filling the all-invoices
// toolbar slot. It OWNS the `invoicePage` URL key (rule 4: which page you are on outlives a click,
// so it lives in the URL, not useState). The all-invoices page reads the same key and asks the
// server for just that slice; nothing is passed between them. Carries the test-contract testids:
// invoice-page-prev, invoice-page-label, invoice-page-next. Page numbers are 1-based; page 1 clears
// the key so the default list URL stays clean, and Previous is disabled on the first page.
function InvoicePagination() {
  const [page, setPage] = useSearchParam('invoicePage', asNumber);
  const currentPage = page ?? 1;
  return (
    <div data-testid="invoice-pagination">
      <button
        type="button"
        data-testid="invoice-page-prev"
        disabled={currentPage <= 1}
        onClick={() => setPage(currentPage <= 2 ? undefined : currentPage - 1)}
      >
        Previous
      </button>
      <span data-testid="invoice-page-label">{currentPage}</span>
      <button
        type="button"
        data-testid="invoice-page-next"
        onClick={() => setPage(currentPage + 1)}
      >
        Next
      </button>
    </div>
  );
}

export const contribution = AllInvoicesToolbar.fill({ order: 20, Component: InvoicePagination });
