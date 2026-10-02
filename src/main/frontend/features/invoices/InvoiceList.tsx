import { Link } from '@tanstack/react-router';
import { InvoiceListToolbar } from '../../slots/defs/invoiceListToolbar';
import { InvoiceRow } from '../../slots/defs/invoiceRow';
import { useSearchParam, asString } from '../../url/useSearchParam';
import { formatMoney } from '../../ui/money';
import { useInvoices, useSendInvoice } from './invoices';

// A project's invoices and what they add up to. Reads its own query key (scoped to the project) and
// derives the total from the rows — the aggregate is never stored, so it always matches the list.
// Each row shows its status, which is WORKED OUT from the payments recorded against the invoice
// (SENT while unpaid, PARTIAL once part paid, PAID once covered), and the control for its next step:
// Send while DRAFT, then Open to record payments — there is no mark-paid-by-hand flip anymore. The
// ordering comes from the shared `invoiceSort` URL key (set by a toolbar control), so the server
// returns the rows already in order.
export function InvoiceList({ projectId }: { projectId: number }) {
  const [sort] = useSearchParam('invoiceSort', asString);
  const { data: invoices } = useInvoices(projectId, sort || undefined);
  const send = useSendInvoice(projectId);

  if (!invoices) {
    return null;
  }

  const total = invoices.reduce((sum, invoice) => sum + Number(invoice.amount), 0);

  return (
    <>
    <InvoiceListToolbar.Slot projectId={projectId} />
    <table data-testid="project-invoices-table">
      <thead>
        <tr>
          <th>Amount</th>
          <th>Issued</th>
          <th>Due</th>
          <th>Left to pay</th>
          <th>Status</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {invoices.map((invoice) => (
          <tr key={invoice.id} data-testid={`invoice-row-${invoice.id}`}>
            <td data-testid="invoice-amount">{formatMoney(invoice.amount)}</td>
            <td data-testid="invoice-issued">{invoice.issuedDate}</td>
            <td data-testid="invoice-due">{invoice.dueDate}</td>
            <td>
              <InvoiceRow.Slot invoice={invoice} />
            </td>
            <td data-testid="invoice-status">{invoice.status}</td>
            <td>
              <Link
                to="/invoices/$invoiceId"
                params={{ invoiceId: String(invoice.id) }}
                data-testid={`invoice-open-${invoice.id}`}
              >
                Open
              </Link>
              {invoice.status === 'DRAFT' && (
                <button
                  type="button"
                  data-testid={`invoice-send-${invoice.id}`}
                  disabled={send.isPending}
                  onClick={() => send.mutate({ id: invoice.id })}
                >
                  Send
                </button>
              )}
            </td>
          </tr>
        ))}
      </tbody>
      <tfoot>
        <tr>
          <td data-testid="project-invoices-total">{formatMoney(total)}</td>
        </tr>
      </tfoot>
    </table>
    </>
  );
}
