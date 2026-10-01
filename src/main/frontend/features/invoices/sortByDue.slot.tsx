import { InvoiceListToolbar } from '../../slots/defs/invoiceListToolbar';
import { useSearchParam, asString } from '../../url/useSearchParam';

// "Sort by due date" — a self-contained control that owns the `invoiceSort` URL key. The invoice
// list reads the same key and asks the server for that ordering; the two share only the key, no
// import. Clicking commits 'due' to the URL (so the ordering outlives the click and is shareable).
function SortByDue() {
  const [, setSort] = useSearchParam('invoiceSort', asString);
  return (
    <button type="button" data-testid="invoice-sort-due" onClick={() => setSort('due')}>
      Sort by due date
    </button>
  );
}

export const contribution = InvoiceListToolbar.fill({ order: 10, Component: SortByDue });
