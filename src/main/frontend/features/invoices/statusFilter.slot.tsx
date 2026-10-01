import { AllInvoicesToolbar } from '../../slots/defs/allInvoicesToolbar';
import { useSearchParam, asString } from '../../url/useSearchParam';

// A control over the all-invoices list — one new *.slot.tsx filling the invoices.toolbar region. It
// owns the `invoiceStatus` URL key; the list reads the same key and asks the server for only that
// stage. Nothing is passed between them: they stay in step through the shared search param. The
// empty option clears the key, so the list falls back to showing every stage.
const STAGES = ['DRAFT', 'SENT', 'PAID'] as const;

function InvoiceStatusFilter() {
  const [status, setStatus] = useSearchParam('invoiceStatus', asString);
  return (
    <select
      data-testid="invoice-status-filter"
      value={status}
      onChange={(e) => setStatus(e.target.value)}
    >
      <option value="">All stages</option>
      {STAGES.map((stage) => (
        <option key={stage} value={stage}>
          {stage}
        </option>
      ))}
    </select>
  );
}

export const contribution = AllInvoicesToolbar.fill({ order: 10, Component: InvoiceStatusFilter });
