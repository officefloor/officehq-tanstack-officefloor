import { formatMoney } from '../../ui/money';
import { useAllInvoices } from './allInvoices';

// One place listing every invoice from every project. Reads its own query key and shows, per row,
// which project the invoice is for and what stage (status) it is at.
export function AllInvoiceList() {
  const { data: invoices } = useAllInvoices();

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
        {invoices.map((invoice) => (
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
