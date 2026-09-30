import { InvoicesToolbar } from '../../slots/defs/invoicesToolbar';
import { asString, useSearchParam } from '../../url/useSearchParam';

// The sort-by-due-date control — its own file, filling the project invoices toolbar slot. It OWNS
// the `invoiceSort` URL key (rule 4: a sort outlives a click, so it lives in the URL, not useState).
// The invoices panel reads the same key and asks the server for the ordered list; nothing is passed
// between them. Carries data-testid="invoice-sort-due" (the test contract).
function SortByDue() {
  const [sort, setSort] = useSearchParam('invoiceSort', asString);
  const active = sort === 'due';
  return (
    <button
      type="button"
      data-testid="invoice-sort-due"
      aria-pressed={active}
      onClick={() => setSort(active ? undefined : 'due')}
    >
      Sort by due date
    </button>
  );
}

export const contribution = InvoicesToolbar.fill({ order: 10, Component: SortByDue });
