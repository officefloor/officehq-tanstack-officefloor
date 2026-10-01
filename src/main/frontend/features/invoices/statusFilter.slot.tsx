import { AllInvoicesToolbar } from '../../slots/defs/allInvoicesToolbar';
import { useSearchParam, asString } from '../../url/useSearchParam';

// "Show one stage" — a self-contained control that owns the `invoiceStatus` URL key. The all-invoices
// list reads the same key and keeps only rows at that stage; the two share only the key, no import.
// Choosing a stage commits it to the URL (so the narrowed view outlives the click and is shareable);
// the empty option clears the key and shows every stage again.
const STAGES = ['DRAFT', 'SENT', 'PAID'] as const;

function StatusFilter() {
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

export const contribution = AllInvoicesToolbar.fill({ order: 10, Component: StatusFilter });
