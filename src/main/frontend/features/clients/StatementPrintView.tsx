import { useQuery } from '@tanstack/react-query';
import {
  clientStatementKey,
  fetchClientStatement,
  type StatementProject,
} from './queries';
import { formatMoney } from '../../ui/money';

// A clean, print-friendly summary of a client's statement: one line per job showing what that job
// still owes, and the single grand total the client owes across every job. Queries for itself under
// ['clients', clientId, 'statement'] (CLAUDE.md rule 5) — the same key the detailed statement reads,
// so the two always agree and a payment invalidating clients refreshes both. Stripped to just the
// headline figures so it reads as a summary you can print, rather than the full invoice breakdown.
export function StatementPrintView({ clientId }: { clientId: number }) {
  const { data: statement } = useQuery({
    queryKey: clientStatementKey(clientId),
    queryFn: () => fetchClientStatement(clientId),
  });

  if (!statement) {
    return null;
  }

  return (
    <section data-testid="statement-print-view">
      <h2>Statement</h2>
      <table data-testid="statement-summary-table">
        <thead>
          <tr>
            <th>Job</th>
            <th>Owed</th>
          </tr>
        </thead>
        <tbody>
          {statement.projects.map((project: StatementProject) => (
            <tr
              key={project.projectId}
              data-testid={`statement-summary-row-${project.projectId}`}
            >
              <td data-testid="statement-summary-job">{project.projectName}</td>
              <td data-testid="statement-summary-owed">{formatMoney(project.subtotal)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p>
        <span>Grand total</span>{' '}
        <span data-testid="statement-grand-total">{formatMoney(statement.totalOwed)}</span>
      </p>
    </section>
  );
}
