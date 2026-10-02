import { useSearchParam, asFlag } from '../../url/useSearchParam';
import { formatMoney } from '../../ui/money';
import { useClients } from './clients';
import { useClientStatement } from './statement';

// A client's statement: all of their invoices gathered in one place, each with what is still due,
// and the grand total they still owe — presented as a clean, printable summary. Whether the
// statement is open lives in the URL (`statement` key, CLAUDE.md rule 4), so it outlives the click
// and is shareable; the open control owns that key.
export function ClientStatement({ clientId }: { clientId: number }) {
  const [open, setOpen] = useSearchParam('statement', asFlag);
  const { data } = useClientStatement(clientId);
  const { data: clients } = useClients();

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

  const client = clients?.find((c) => c.id === clientId);

  return (
    <article data-testid="statement-print-view" className="statement-print-view">
      <header>
        <h2>Statement</h2>
        {client ? (
          <p>
            <span data-testid="statement-client-name">{client.name}</span>
            {' — '}
            <span data-testid="statement-client-email">{client.email}</span>
          </p>
        ) : null}
      </header>
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
      <footer>
        <p className="statement-grand-total-line">
          Grand total owed:{' '}
          <strong data-testid="statement-grand-total">
            {formatMoney(data.outstandingTotal)}
          </strong>
        </p>
      </footer>
    </article>
  );
}
