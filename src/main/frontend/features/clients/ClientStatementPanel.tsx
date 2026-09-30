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
        <tbody>
          {invoices.map((i) => (
            <tr key={i.id} data-testid={`statement-invoice-row-${i.id}`}>
              <td data-testid="statement-invoice-amount">{money(Number(i.amount))}</td>
              <td data-testid="statement-invoice-due">{money(Number(i.amountDue))}</td>
              <td data-testid="statement-invoice-issued">{i.issuedDate}</td>
              <td data-testid="statement-invoice-dueDate">{i.dueDate}</td>
              <td data-testid="statement-invoice-status">{invoiceStatus(i)}</td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr>
            <td>Total still owed</td>
            <td data-testid="client-outstanding-total">{money(outstanding)}</td>
          </tr>
        </tfoot>
      </table>
    </section>
  );
}
