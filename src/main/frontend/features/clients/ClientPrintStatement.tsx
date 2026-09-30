import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { money } from '../../ui/money';

// A clean, printable summary of a client's statement: every invoice raised against the client in one
// plain list, and — the headline — the grand total the client still owes. The server derives that
// total (each invoice's amount less its payments, summed); this view only presents it, so the figure
// is the same one the statement panel and dashboard show, never recomputed on the client (rule 5).
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

// The printable summary in the client-detail context — its own file, filling the client-detail slot
// below the statement. It shows once the statement is opened, reading the `statement` URL key that
// features/clients/statementOpen.slot.tsx owns (rule 4); nothing is passed between them. Server data
// is read with useQuery under the ['invoices', 'statement', clientId] key the statement panel uses,
// so a payment or invoice write that invalidates ['invoices'] refreshes this summary too (rule 5).
export function ClientPrintStatement({ clientId }: { clientId: number }) {
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
  const grandTotal = data ? Number(data.outstanding) : 0;

  return (
    <section data-testid="statement-print-view" aria-label="Printable statement summary">
      <h2>Statement summary</h2>
      <table data-testid="statement-print-table">
        <thead>
          <tr>
            <th>Invoice</th>
            <th>Issued</th>
            <th>Due</th>
            <th>Amount owed</th>
          </tr>
        </thead>
        <tbody>
          {invoices.map((i) => (
            <tr key={i.id} data-testid={`statement-print-row-${i.id}`}>
              <td>{i.id}</td>
              <td>{i.issuedDate}</td>
              <td>{i.dueDate}</td>
              <td data-testid={`statement-print-amount-${i.id}`}>{money(Number(i.amountDue))}</td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr>
            <td colSpan={3}>Grand total owed</td>
            <td data-testid="statement-grand-total">{money(grandTotal)}</td>
          </tr>
        </tfoot>
      </table>
    </section>
  );
}
