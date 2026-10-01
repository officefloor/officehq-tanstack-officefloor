import { InvoiceListToolbar } from '../../slots/defs/invoiceListToolbar';
import { useSearchParam, asString } from '../../url/useSearchParam';
import { formatMoney } from '../../ui/money';
import { useInvoices, usePayInvoice } from './invoices';

// A project's invoices and what they add up to. Reads its own query key (scoped to the project) and
// derives the total from the rows — the aggregate is never stored, so it always matches the list.
// Each row shows its payment status and, while UNPAID, a control to mark it paid. The ordering comes
// from the shared `invoiceSort` URL key (set by a toolbar control), so the server returns the rows
// already in order.
export function InvoiceList({ projectId }: { projectId: number }) {
  const [sort] = useSearchParam('invoiceSort', asString);
  const { data: invoices } = useInvoices(projectId, sort || undefined);
  const pay = usePayInvoice(projectId);

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
            <td data-testid="invoice-status">{invoice.status}</td>
            <td>
              {invoice.status !== 'PAID' && (
                <button
                  type="button"
                  data-testid={`invoice-pay-${invoice.id}`}
                  disabled={pay.isPending}
                  onClick={() => pay.mutate({ id: invoice.id })}
                >
                  Mark paid
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
