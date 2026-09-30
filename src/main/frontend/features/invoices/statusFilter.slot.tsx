import { AllInvoicesToolbar } from '../../slots/defs/allInvoicesToolbar';
import { asString, useSearchParam } from '../../url/useSearchParam';

// The status filter — its own file, filling the all-invoices toolbar slot. It OWNS the
// `invoiceStatus` URL key (rule 4: a filter outlives a click, so it lives in the URL, not
// useState). The all-invoices page reads the same key and asks the server for the narrowed list;
// nothing is passed between them. Carries data-testid="invoice-status-filter" (the test contract).
// The empty option clears the key, showing every stage again.
const STAGES = ['DRAFT', 'SENT', 'PAID'];

function StatusFilter() {
  const [status, setStatus] = useSearchParam('invoiceStatus', asString);
  return (
    <select
      data-testid="invoice-status-filter"
      value={status}
      onChange={(e) => setStatus(e.target.value || undefined)}
    >
      <option value="">All stages</option>
      {STAGES.map((s) => (
        <option key={s} value={s}>
          {s}
        </option>
      ))}
    </select>
  );
}

export const contribution = AllInvoicesToolbar.fill({ order: 10, Component: StatusFilter });
