import { useQuery } from '@tanstack/react-query';
import { ProjectDetail } from '../../slots/defs/projectDetail';
import { InvoiceRow } from '../../slots/defs/invoiceRow';
import { invoicesKey, listInvoices, type Invoice, type InvoiceSort } from './api';
import { formatMoney } from '../../ui/money';
import { useSearchParam, asString } from '../../url/useSearchParam';

// The project's invoices and what they add up to — one panel filling the project.detail region.
// Reads server data under ['invoices', projectId] (never copied into state); the add form shares
// the key, so a successful add refreshes this with no import between them. The total is DERIVED
// from the same query (never stored), and amounts render with two decimals.
function ProjectInvoices({ projectId }: { projectId: number }) {
  // The ordering lives in the URL under the shared `invoiceSort` key — the sort control writes it,
  // this list reads it. It is part of the query key, so each ordering caches on its own and the
  // server returns the rows already sorted.
  const [sortParam] = useSearchParam('invoiceSort', asString);
  const sort: InvoiceSort = sortParam === 'due' ? 'due' : 'id';
  const { data: invoices } = useQuery({
    queryKey: [...invoicesKey(projectId), sort],
    queryFn: () => listInvoices(projectId, sort),
  });

  if (!invoices) {
    return null;
  }

  const total = invoices.reduce((sum, invoice) => sum + Number(invoice.amount), 0);

  return (
    <>
      <table data-testid="project-invoices-table">
        <thead>
          <tr>
            <th>Amount</th>
            <th>Status</th>
            <th>Issued</th>
            <th>Due</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {invoices.map((invoice: Invoice) => (
            <tr key={invoice.id} data-testid={`invoice-row-${invoice.id}`}>
              <td data-testid="invoice-amount">{formatMoney(invoice.amount)}</td>
              <td data-testid="invoice-status">{invoice.status}</td>
              <td data-testid="invoice-issued">{invoice.issuedDate}</td>
              <td data-testid="invoice-due">{invoice.dueDate}</td>
              <td>
                <InvoiceRow.Slot invoiceId={invoice.id} projectId={projectId} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <p data-testid="project-invoices-total">{formatMoney(total)}</p>
    </>
  );
}

export const contribution = ProjectDetail.fill({ order: 20, Component: ProjectInvoices });
