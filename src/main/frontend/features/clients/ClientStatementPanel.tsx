import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { money } from '../../ui/money';
import { invoiceStatus } from '../../ui/invoiceStatus';

// A client's statement as the server returns it: all of that client's invoices in one place — every
// invoice raised against any of the client's projects, each carrying its derived amountDue (its
// amount minus every payment recorded against it) — and the outstanding total the client still owes
// (the sum of those amounts due).
type StatementInvoice = {
  id: number;
  projectId: number;
  amount: number;
  status: string;
  issuedDate: string;
  dueDate: string;
  amountDue: number;
};
type Statement = {
  invoices: StatementInvoice[];
  outstanding: number;
  // The currency the whole statement is shown in — this client's billing currency (USD or EUR).
  currency: string;
};

// A client's statement, rendered in the client-detail context: all of the client's invoices in one
// place with the total they still owe. It only shows once the statement is opened — the `statement`
// URL key, owned by features/clients/statementOpen.slot.tsx, which this reads (rule 4). Server data
// is read with useQuery under a key that starts with ['invoices'], so a payment or invoice write
// that invalidates ['invoices'] refreshes the statement too — never copied into state, never
// hand-maintained (rule 5). The outstanding total is server-derived (like the dashboard's).
export function ClientStatementPanel({ clientId }: { clientId: number }) {
  const [statement] = useSearchParam('statement', asString);
  const open = statement === 'open';

  const query = useQuery({
    queryKey: ['invoices', 'statement', clientId],
    queryFn: () => getJson<Statement>(`/api/clients/statement?clientId=${clientId}`),
    enabled: open,
  });

  if (!open) {
    return null;
  }

  const data = query.data;
  const invoices = data?.invoices ?? [];
  const outstanding = data ? Number(data.outstanding) : 0;
  const currency = data?.currency ?? 'USD';

  // Group the client's invoices by the job (project) they were raised against, preserving the
  // server's oldest-first order both across jobs (first job to appear stays first) and within each
  // job. Each job carries a subtotal — the sum of its invoices' amounts due — so the statement reads
  // per job while the outstanding total across every job is unchanged (still server-derived).
  const groups: { projectId: number; invoices: StatementInvoice[]; subtotal: number }[] = [];
  const groupByProject = new Map<number, (typeof groups)[number]>();
  for (const invoice of invoices) {
    let group = groupByProject.get(invoice.projectId);
    if (!group) {
      group = { projectId: invoice.projectId, invoices: [], subtotal: 0 };
      groupByProject.set(invoice.projectId, group);
      groups.push(group);
    }
    group.invoices.push(invoice);
    group.subtotal += Number(invoice.amountDue);
  }

  return (
    <section data-testid="client-statement">
      <table data-testid="client-statement-table">
        <thead>
          <tr>
            <th>Amount</th>
            <th>Amount due</th>
            <th>Issued</th>
            <th>Due</th>
            <th>Status</th>
          </tr>
        </thead>
        {groups.map((group) => (
          <tbody key={group.projectId} data-testid={`statement-project-${group.projectId}`}>
            {group.invoices.map((i) => (
              <tr key={i.id} data-testid={`statement-invoice-row-${i.id}`}>
                <td data-testid="statement-invoice-amount">{money(Number(i.amount), currency)}</td>
                <td data-testid="statement-invoice-due">{money(Number(i.amountDue), currency)}</td>
                <td data-testid="statement-invoice-issued">{i.issuedDate}</td>
                <td data-testid="statement-invoice-dueDate">{i.dueDate}</td>
                <td data-testid="statement-invoice-status">{invoiceStatus(i)}</td>
              </tr>
            ))}
            <tr>
              <td>Job subtotal</td>
              <td data-testid="statement-project-subtotal">{money(group.subtotal, currency)}</td>
            </tr>
          </tbody>
        ))}
        <tfoot>
          <tr>
            <td>Total still owed</td>
            <td data-testid="client-outstanding-total">{money(outstanding, currency)}</td>
          </tr>
        </tfoot>
      </table>
    </section>
  );
}
