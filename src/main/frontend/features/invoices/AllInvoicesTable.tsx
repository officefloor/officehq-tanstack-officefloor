import { useQuery } from '@tanstack/react-query';
import { allInvoicesKey, fetchAllInvoices, type AllInvoice } from './queries';
import { formatMoney } from '../../ui/money';

// The one place listing every invoice from every project. Queries for itself under ['invoices',
// 'all'] — never handed its data by a parent (CLAUDE.md rule 5). Each row shows the invoice's
// project NAME (from the server-side join) and its lifecycle stage.
export function AllInvoicesTable() {
  const { data: invoices } = useQuery({ queryKey: allInvoicesKey, queryFn: fetchAllInvoices });

  if (!invoices) {
    return null;
  }

  if (invoices.length === 0) {
    return <p data-testid="all-invoices-empty">No invoices yet.</p>;
  }

  return (
    <table data-testid="all-invoices-table">
      <thead>
        <tr>
          <th>Project</th>
          <th>Amount</th>
          <th>Stage</th>
        </tr>
      </thead>
      <tbody>
        {invoices.map((invoice: AllInvoice) => (
          <tr key={invoice.id} data-testid={`invoice-row-${invoice.id}`}>
            <td data-testid="invoice-project">{invoice.projectName}</td>
            <td data-testid="invoice-amount">{formatMoney(invoice.amount)}</td>
            <td data-testid="invoice-status">{invoice.status}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
