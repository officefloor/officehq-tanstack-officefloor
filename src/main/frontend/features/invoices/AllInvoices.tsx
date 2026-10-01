import { useQuery } from '@tanstack/react-query';
import { allInvoicesKey, listAllInvoices, type AllInvoice } from './allInvoices';
import { formatMoney } from '../../ui/money';

// One place listing every invoice across all projects. Reads server data under ['invoices', 'all']
// (never copied into state); each row shows which PROJECT the invoice is for (the name the server
// joins on) and what STAGE it is at (its status). Amounts render with two decimals.
export function AllInvoices() {
  const { data: invoices } = useQuery({ queryKey: allInvoicesKey, queryFn: listAllInvoices });

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
          <th>Status</th>
          <th>Amount</th>
        </tr>
      </thead>
      <tbody>
        {invoices.map((invoice: AllInvoice) => (
          <tr key={invoice.id} data-testid={`invoice-row-${invoice.id}`}>
            <td data-testid="invoice-project">{invoice.projectName}</td>
            <td data-testid="invoice-status">{invoice.status}</td>
            <td data-testid="invoice-amount">{formatMoney(invoice.amount)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
