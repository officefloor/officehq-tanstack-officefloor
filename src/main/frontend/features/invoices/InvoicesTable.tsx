import { useQuery } from '@tanstack/react-query';
import { invoicesKey, fetchInvoices, type Invoice } from './queries';
import { InvoiceRow } from '../../slots/defs/invoiceRow';

// A project's invoices list plus the derived total. Queries for itself under ['invoices', projectId]
// — never handed its data by a parent (CLAUDE.md rule 5). Amounts render with 2 decimals; the total
// is a client-side aggregate of the same rows, so it stays in step with them for free.
export function InvoicesTable({ projectId }: { projectId: number }) {
  const { data: invoices } = useQuery({
    queryKey: invoicesKey(projectId),
    queryFn: () => fetchInvoices(projectId),
  });

  if (!invoices) {
    return null;
  }

  const total = invoices.reduce((sum, invoice) => sum + Number(invoice.amount), 0);

  return (
    <>
      <table data-testid="project-invoices-table">
        <thead>
          <tr>
            <th>Amount</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {invoices.map((invoice: Invoice) => (
            <tr key={invoice.id} data-testid={`invoice-row-${invoice.id}`}>
              <td data-testid="invoice-amount">{Number(invoice.amount).toFixed(2)}</td>
              <InvoiceRow.Slot
                invoiceId={invoice.id}
                projectId={projectId}
                status={invoice.status}
              />
            </tr>
          ))}
        </tbody>
      </table>
      <p data-testid="project-invoices-total">{total.toFixed(2)}</p>
    </>
  );
}
