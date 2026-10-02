import { useSearchParam, asFlag } from '../../url/useSearchParam';
import { formatMoney } from '../../ui/money';
import { useClientStatement } from './statement';

// A client's statement: all of their invoices gathered in one place, each with what is still due,
// and the total they still owe. Whether the statement is open lives in the URL (`statement` key,
// CLAUDE.md rule 4), so it outlives the click and is shareable; the open control owns that key.
export function ClientStatement({ clientId }: { clientId: number }) {
  const [open, setOpen] = useSearchParam('statement', asFlag);
  const { data } = useClientStatement(clientId);

  if (!open) {
    return (
      <button type="button" data-testid="client-statement-open" onClick={() => setOpen(true)}>
        Statement
      </button>
    );
  }

  if (!data) {
    return null;
  }

  return (
    <section data-testid="client-statement">
      {data.projects.map((project) => (
        <div key={project.projectId} data-testid={`statement-project-${project.projectId}`}>
          <h3 data-testid="statement-project-name">{project.name}</h3>
          <table data-testid="client-statement-table">
            <thead>
              <tr>
                <th>Invoice</th>
                <th>Amount</th>
                <th>Paid</th>
                <th>Due</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {project.invoices.map((invoice) => (
                <tr key={invoice.id} data-testid={`statement-invoice-row-${invoice.id}`}>
                  <td data-testid="statement-invoice-id">{invoice.id}</td>
                  <td data-testid="statement-invoice-amount">{formatMoney(invoice.amount)}</td>
                  <td data-testid="statement-invoice-paid">{formatMoney(invoice.paid)}</td>
                  <td data-testid="statement-invoice-due">{formatMoney(invoice.due)}</td>
                  <td data-testid="statement-invoice-status">{invoice.status}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <p>
            Subtotal:{' '}
            <strong data-testid="statement-project-subtotal">{formatMoney(project.subtotal)}</strong>
          </p>
        </div>
      ))}
      <p>
        Total owed:{' '}
        <strong data-testid="client-outstanding-total">
          {formatMoney(data.outstandingTotal)}
        </strong>
      </p>
    </section>
  );
}
