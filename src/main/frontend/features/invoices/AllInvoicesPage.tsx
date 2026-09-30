import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
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
  const invoices = useQuery({
    queryKey: ['invoices', 'all'],
    queryFn: () => getJson<InvoiceView[]>('/api/invoices/all'),
  });

  const rows = invoices.data ?? [];

  return (
    <section data-testid="all-invoices">
      {rows.length === 0 ? (
        <p data-testid="all-invoices-empty">No invoices yet.</p>
      ) : (
        <table data-testid="all-invoices-table">
          <thead>
            <tr>
              <th>Project</th>
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
