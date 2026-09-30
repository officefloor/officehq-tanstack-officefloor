import { InvoicesToolbar } from '../../slots/defs/invoicesToolbar';
import { useSearchParam, asString } from '../../url/useSearchParam';
import { INVOICE_SORT_PARAM } from './queries';

// The "sort by due date" control — its own file filling the invoices.toolbar region (CLAUDE.md
// rule 3). It owns the `invoiceSort` URL key (rule 4): the chosen sort outlives a click, so it lives
// in the URL, and the list (InvoicesTable) reads the very same key to order its rows. No callback,
// no shared state — just the key.
function InvoiceSortByDue() {
  const [sort, setSort] = useSearchParam(INVOICE_SORT_PARAM, asString);
  return (
    <button
      type="button"
      data-testid="invoice-sort-due"
      aria-pressed={sort === 'due'}
      onClick={() => setSort('due')}
    >
      Sort by due date
    </button>
  );
}

export const contribution = InvoicesToolbar.fill({ order: 10, Component: InvoiceSortByDue });
