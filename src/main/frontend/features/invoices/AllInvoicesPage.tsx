import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asNumber, asString, useSearchParam } from '../../url/useSearchParam';
import { AllInvoicesToolbar } from '../../slots/defs/allInvoicesToolbar';
import { money } from '../../ui/money';

// An invoice as the all-invoices endpoint returns it: which project it is for (by NAME, joined
// server-side), its amount, and its lifecycle status (the stage it is at). See InvoicesAllGet.
export type InvoiceView = {
  id: number;
  projectId: number;
  projectName: string;
  amount: number;
  status: string;
  issuedDate: string;
  dueDate: string;
};

// One place listing every invoice across all projects, each row showing the project it is for and
// the stage it is at. Server data is read with useQuery under ['invoices', 'all'] — the key still
// starts with 'invoices', so a write that invalidates ['invoices'] refreshes this list too (rule 5).
export function AllInvoicesPage() {
  // The chosen stage lives in the URL (owned by features/invoices/statusFilter.slot.tsx). The page
  // reads the same key and asks the server for the narrowed list — a single source of truth, no
  // server data copied into state or filtered by hand. The key still starts with 'invoices', so a
  // write that invalidates ['invoices'] refreshes this list too (rule 5).
  const [status] = useSearchParam('invoiceStatus', asString);
  // The huge list is shown a page at a time. The chosen page lives in the URL too (owned by
  // features/invoices/pagination.slot.tsx, which renders the next/previous controls); the page reads
  // the same key and asks the server for just that slice — the same single-source-of-truth shape as
  // the status filter above, no rows sliced or held in state here.
  const [page] = useSearchParam('invoicePage', asNumber);
  const currentPage = page ?? 1;
  const invoices = useQuery({
    queryKey: ['invoices', 'all', status, currentPage],
    queryFn: () => {
      const params = new URLSearchParams({ page: String(currentPage) });
      if (status) params.set('status', status);
      return getJson<InvoiceView[]>(`/api/invoices/all?${params.toString()}`);
    },
  });

  const rows = invoices.data ?? [];

  return (
    <section data-testid="all-invoices">
      <div data-testid="all-invoices-toolbar">
        <AllInvoicesToolbar.Slot />
      </div>
      {rows.length === 0 ? (
        <p data-testid="all-invoices-empty">No invoices yet.</p>
      ) : (
        <table data-testid="all-invoices-table">
          <thead>
            <tr>
              <th>Job</th>
              <th>Amount</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((i) => (
              <tr key={i.id} data-testid={`invoice-row-${i.id}`}>
                <td data-testid="invoice-project">{i.projectName}</td>
                <td data-testid="invoice-amount">{money(Number(i.amount))}</td>
                <td data-testid="invoice-status">{i.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
