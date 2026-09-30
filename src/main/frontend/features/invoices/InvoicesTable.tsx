import { useQuery } from '@tanstack/react-query';
import { invoicesKey, fetchInvoices, INVOICE_SORT_PARAM, type Invoice } from './queries';
import { InvoiceRow } from '../../slots/defs/invoiceRow';
import { InvoicesToolbar } from '../../slots/defs/invoicesToolbar';
import { useSearchParam, asString } from '../../url/useSearchParam';
import { formatMoney } from '../../ui/money';

// A project's invoices list plus the derived total. Queries for itself under ['invoices', projectId]
// — never handed its data by a parent (CLAUDE.md rule 5). Amounts render with 2 decimals; the total
// is a client-side aggregate of the same rows, so it stays in step with them for free.
export function InvoicesTable({ projectId }: { projectId: number }) {
  const { data: invoices } = useQuery({
    queryKey: invoicesKey(projectId),
    queryFn: () => fetchInvoices(projectId),
  });
  // Which sort is active lives in the URL (CLAUDE.md rule 4); the sort control owns the key and this
  // list reads it. Due dates are stored as ISO strings, so a plain string compare is chronological.
  const [sort] = useSearchParam(INVOICE_SORT_PARAM, asString);

  if (!invoices) {
    return null;
  }

  const rows =
    sort === 'due'
      ? [...invoices].sort((a, b) => (a.dueDate ?? '').localeCompare(b.dueDate ?? ''))
      : invoices;

  const total = invoices.reduce((sum, invoice) => sum + Number(invoice.amount), 0);

  return (
    <>
      <InvoicesToolbar.Slot projectId={projectId} />
      <table data-testid="project-invoices-table">
        <thead>
          <tr>
            <th>Amount</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {rows.map((invoice: Invoice) => (
            <tr key={invoice.id} data-testid={`invoice-row-${invoice.id}`}>
              <td data-testid="invoice-amount">{formatMoney(invoice.amount)}</td>
              <InvoiceRow.Slot
                invoiceId={invoice.id}
                projectId={projectId}
                status={invoice.status}
              />
            </tr>
          ))}
        </tbody>
      </table>
      <p data-testid="project-invoices-total">{formatMoney(total)}</p>
    </>
  );
}
