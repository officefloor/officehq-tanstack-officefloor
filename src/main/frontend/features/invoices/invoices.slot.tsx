import { useQuery } from '@tanstack/react-query';
import { ProjectDetail } from '../../slots/defs/projectDetail';
import { InvoiceRow } from '../../slots/defs/invoiceRow';
import { invoicesKey, listInvoices, type Invoice } from './api';
import { formatMoney } from '../../ui/money';

// The project's invoices and what they add up to — one panel filling the project.detail region.
// Reads server data under ['invoices', projectId] (never copied into state); the add form shares
// the key, so a successful add refreshes this with no import between them. The total is DERIVED
// from the same query (never stored), and amounts render with two decimals.
function ProjectInvoices({ projectId }: { projectId: number }) {
  const { data: invoices } = useQuery({
    queryKey: invoicesKey(projectId),
    queryFn: () => listInvoices(projectId),
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
