import { ProjectDetail } from '../../slots/defs/projectDetail';
import { useSearchParam, asString } from '../../url/useSearchParam';

// A toolbar control over the project's invoices table — one new *.slot.tsx filling the project.detail
// region. It owns the `invoiceSort` URL key; the invoices list reads the same key and asks the server
// for that order. Nothing is passed between them: they stay in step through the shared search param.
function SortInvoicesByDue() {
  const [sort, setSort] = useSearchParam('invoiceSort', asString);
  const active = sort === 'due';
  return (
    <button
      data-testid="invoice-sort-due"
      type="button"
      aria-pressed={active}
      onClick={() => setSort(active ? undefined : 'due')}
    >
      Sort by due date
    </button>
  );
}

export const contribution = ProjectDetail.fill({ order: 15, Component: SortInvoicesByDue });
