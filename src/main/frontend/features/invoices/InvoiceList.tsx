import { useInvoices } from './invoices';

// A project's invoices and what they add up to. Reads its own query key (scoped to the project) and
// derives the total from the rows — the aggregate is never stored, so it always matches the list.
export function InvoiceList({ projectId }: { projectId: number }) {
  const { data: invoices } = useInvoices(projectId);

  if (!invoices) {
    return null;
  }

  const total = invoices.reduce((sum, invoice) => sum + Number(invoice.amount), 0);

  return (
    <table data-testid="project-invoices-table">
      <thead>
        <tr>
          <th>Amount</th>
        </tr>
      </thead>
      <tbody>
        {invoices.map((invoice) => (
          <tr key={invoice.id} data-testid={`invoice-row-${invoice.id}`}>
            <td data-testid="invoice-amount">{Number(invoice.amount).toFixed(2)}</td>
          </tr>
        ))}
      </tbody>
      <tfoot>
        <tr>
          <td data-testid="project-invoices-total">{total.toFixed(2)}</td>
        </tr>
      </tfoot>
    </table>
  );
}
