import { useQuery } from '@tanstack/react-query';
import { clientStatementKey, fetchClientStatement, type StatementInvoice } from './queries';
import { formatMoney } from '../../ui/money';

// A client's statement: all their invoices in one place with the total still owed. Queries for
// itself under ['clients', clientId, 'statement'] — never handed its data by a parent (CLAUDE.md
// rule 5). The per-invoice due and the total owed are derived server-side, so the headline figure
// always matches the rows.
export function ClientStatementTable({ clientId }: { clientId: number }) {
  const { data: statement } = useQuery({
    queryKey: clientStatementKey(clientId),
    queryFn: () => fetchClientStatement(clientId),
  });

  if (!statement) {
    return null;
  }

  return (
    <>
      <table data-testid="client-statement-table">
        <thead>
          <tr>
            <th>Amount</th>
            <th>Status</th>
            <th>Due</th>
          </tr>
        </thead>
        <tbody>
          {statement.invoices.map((invoice: StatementInvoice) => (
            <tr key={invoice.id} data-testid={`statement-invoice-row-${invoice.id}`}>
              <td data-testid="statement-invoice-amount">{formatMoney(invoice.amount)}</td>
              <td data-testid="statement-invoice-status">{invoice.status}</td>
              <td data-testid="statement-invoice-due">{formatMoney(invoice.due)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p data-testid="client-outstanding-total">{formatMoney(statement.totalOwed)}</p>
    </>
  );
}
