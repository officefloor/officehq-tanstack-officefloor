import { useQuery } from '@tanstack/react-query';
import { allInvoicesKey, listAllInvoices, type AllInvoice } from './allInvoices';
import { formatMoney } from '../../ui/money';
import { AllInvoicesToolbar } from '../../slots/defs/allInvoicesToolbar';
import { useSearchParam, asString } from '../../url/useSearchParam';

// One place listing every invoice across all projects. Reads server data under ['invoices', 'all']
// (never copied into state); each row shows which PROJECT the invoice is for (the name the server
// joins on) and what STAGE it is at (its status). Amounts render with two decimals.
//
// The chosen stage lives in the URL under the shared `invoiceStatus` key — the filter control (its
// own *.slot.tsx) writes it, this list reads it. It is part of the query key, so each stage caches
// on its own and the server returns the rows already narrowed.
export function AllInvoices() {
  const [status] = useSearchParam('invoiceStatus', asString);
  const { data: invoices } = useQuery({
    queryKey: [...allInvoicesKey, status],
    queryFn: () => listAllInvoices(status),
  });

  return (
    <>
      <AllInvoicesToolbar.Slot />
      <AllInvoicesList invoices={invoices} />
    </>
  );
}

function AllInvoicesList({ invoices }: { invoices: AllInvoice[] | undefined }) {
  if (!invoices) {
    return null;
  }

  if (invoices.length === 0) {
    return <p data-testid="all-invoices-empty">No invoices yet.</p>;
  }

  return (
    <table data-testid="all-invoices-table">
      <thead>
        <tr>
          <th>Project</th>
          <th>Status</th>
          <th>Amount</th>
        </tr>
      </thead>
      <tbody>
        {invoices.map((invoice: AllInvoice) => (
          <tr key={invoice.id} data-testid={`invoice-row-${invoice.id}`}>
            <td data-testid="invoice-project">{invoice.projectName}</td>
            <td data-testid="invoice-status">{invoice.status}</td>
            <td data-testid="invoice-amount">{formatMoney(invoice.amount)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
