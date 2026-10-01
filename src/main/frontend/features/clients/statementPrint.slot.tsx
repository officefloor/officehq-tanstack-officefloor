import { useQuery } from '@tanstack/react-query';
import { ClientDetail } from '../../slots/defs/clientDetail';
import { useSearchParam, asFlag } from '../../url/useSearchParam';
import {
  clientStatementKey,
  getClientStatement,
  type StatementInvoice,
  type StatementProject,
} from './statementApi';
import { formatMoney } from '../../ui/money';

// A clean, printable summary of a client's statement — one new *.slot.tsx filling the client.detail
// region. It shows when the shared `statement` URL flag is on (owned by the statement-open control),
// so clicking Statement reveals the print view with no flag or callback passed between them. The
// panel queries the statement for itself under ['clients', clientId, 'statement'] (never copies it
// into state); the grand total they owe is the server-derived outstanding total, formatted once.
function StatementPrint({ clientId }: { clientId: number }) {
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
    <section data-testid="statement-print-view">
      <h3>Statement</h3>
      <table data-testid="statement-print-table">
        <thead>
          <tr>
            <th>Invoice</th>
            <th>Project</th>
            <th>Status</th>
            <th>Amount owed</th>
          </tr>
        </thead>
        <tbody>
          {statement.projects.flatMap((project: StatementProject) =>
            project.invoices.map((invoice: StatementInvoice) => (
              <tr key={invoice.id} data-testid={`statement-print-row-${invoice.id}`}>
                <td data-testid="statement-print-invoice-id">{invoice.id}</td>
                <td data-testid="statement-print-project-name">{project.name}</td>
                <td data-testid="statement-print-invoice-status">{invoice.status}</td>
                <td data-testid="statement-print-invoice-due">{formatMoney(invoice.amountDue, statement.currency)}</td>
              </tr>
            )),
          )}
        </tbody>
      </table>
      <p>
        Grand total owed:{' '}
        <strong data-testid="statement-grand-total">
          {formatMoney(statement.outstandingTotal, statement.currency)}
        </strong>
      </p>
    </section>
  );
}

export const contribution = ClientDetail.fill({ order: 40, Component: StatementPrint });
