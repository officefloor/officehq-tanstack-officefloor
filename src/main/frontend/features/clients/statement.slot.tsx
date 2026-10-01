import { useQuery } from '@tanstack/react-query';
import { ClientDetail } from '../../slots/defs/clientDetail';
import { useSearchParam, asFlag } from '../../url/useSearchParam';
import { clientStatementKey, getClientStatement, type StatementInvoice } from './statementApi';
import { formatMoney } from '../../ui/money';

// A client's statement — one panel filling the client.detail region: all of the client's invoices
// in one place, with the total they still owe. It only renders when the `statement` URL flag is on
// (owned by the statement-open control), so the two stay in step through the shared search param.
// Server data is read under ['clients', clientId, 'statement'] (never copied into state): the panel
// queries for itself, and the server does the cross-project join and derives each balance.
function ClientStatement({ clientId }: { clientId: number }) {
  const [open] = useSearchParam('statement', asFlag);
  const { data: statement } = useQuery({
    queryKey: clientStatementKey(clientId),
    queryFn: () => getClientStatement(clientId),
    enabled: open,
  });

  if (!open || !statement) {
    return null;
  }

  return (
    <section data-testid="client-statement">
      <table data-testid="client-statement-table">
        <thead>
          <tr>
            <th>Invoice</th>
            <th>Status</th>
            <th>Amount</th>
            <th>Due</th>
          </tr>
        </thead>
        <tbody>
          {statement.invoices.map((invoice: StatementInvoice) => (
            <tr key={invoice.id} data-testid={`statement-invoice-row-${invoice.id}`}>
              <td data-testid="statement-invoice-id">{invoice.id}</td>
              <td data-testid="statement-invoice-status">{invoice.status}</td>
              <td data-testid="statement-invoice-amount">{formatMoney(invoice.amount)}</td>
              <td data-testid="statement-invoice-due">{formatMoney(invoice.amountDue)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p>
        Total owed:{' '}
        <span data-testid="client-outstanding-total">
          {formatMoney(statement.outstandingTotal)}
        </span>
      </p>
    </section>
  );
}

export const contribution = ClientDetail.fill({ order: 30, Component: ClientStatement });
