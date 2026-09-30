import { AllInvoicesToolbar } from '../../slots/defs/allInvoicesToolbar';
import { useSearchParam, asString } from '../../url/useSearchParam';
import { INVOICE_STATUS_PARAM, INVOICE_STATUSES } from './queries';

// The "narrow to one stage" control — its own file filling the all-invoices toolbar region
// (CLAUDE.md rule 3). It owns the `invoiceStatus` URL key (rule 4): the chosen stage outlives a
// click, so it lives in the URL, and the list (AllInvoicesTable) reads the very same key to drop
// rows at other stages. No callback, no shared state — just the key.
function InvoiceStatusFilter() {
  const [status, setStatus] = useSearchParam(INVOICE_STATUS_PARAM, asString);
  return (
    <select
      data-testid="invoice-status-filter"
      value={status}
      onChange={(e) => setStatus(e.target.value)}
    >
      <option value="">All stages</option>
      {INVOICE_STATUSES.map((s) => (
        <option key={s} value={s}>
          {s}
        </option>
      ))}
    </select>
  );
}

export const contribution = AllInvoicesToolbar.fill({ order: 10, Component: InvoiceStatusFilter });
