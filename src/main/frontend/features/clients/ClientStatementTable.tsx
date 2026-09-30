import { useQuery } from '@tanstack/react-query';
import {
  clientStatementKey,
  fetchClientStatement,
  type StatementInvoice,
  type StatementProject,
} from './queries';
import { formatMoney } from '../../ui/money';

// A client's statement: their invoices grouped by JOB, with a subtotal per job, plus the total still
// owed across every job. Queries for itself under ['clients', clientId, 'statement'] — never handed
// its data by a parent (CLAUDE.md rule 5). The per-invoice due, each job's subtotal and the total
// owed are all derived server-side, so the figures always agree with the rows.
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
      {statement.projects.map((project: StatementProject) => (
        <section
          key={project.projectId}
          data-testid={`statement-project-${project.projectId}`}
        >
          <h3 data-testid="statement-project-name">{project.projectName}</h3>
          <table data-testid="statement-project-table">
            <thead>
              <tr>
                <th>Amount</th>
                <th>Status</th>
                <th>Due</th>
              </tr>
            </thead>
            <tbody>
              {project.invoices.map((invoice: StatementInvoice) => (
                <tr key={invoice.id} data-testid={`statement-invoice-row-${invoice.id}`}>
                  <td data-testid="statement-invoice-amount">
                    {formatMoney(invoice.amount, statement.currency)}
                  </td>
                  <td data-testid="statement-invoice-status">{invoice.status}</td>
                  <td data-testid="statement-invoice-due">
                    {formatMoney(invoice.due, statement.currency)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <p data-testid="statement-project-subtotal">
            {formatMoney(project.subtotal, statement.currency)}
          </p>
        </section>
      ))}
      <p data-testid="client-outstanding-total">
        {formatMoney(statement.totalOwed, statement.currency)}
      </p>
    </>
  );
}
