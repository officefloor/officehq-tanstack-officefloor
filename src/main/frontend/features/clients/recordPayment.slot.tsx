import { ClientDetail } from '../../slots/defs/clientDetail';
import { useSearchParam, asFlag } from '../../url/useSearchParam';

// The control that opens the split-payment form — one new *.slot.tsx filling the client.detail
// region. Showing the form is a show/hide toggle, so it lives in the URL: this button owns the
// `payment` key, and the form panel reads the same key. Nothing is passed between them; they stay
// in step through the shared search param. Its testid is client-record-payment (the test contract).
function RecordPayment() {
  const [open, setOpen] = useSearchParam('payment', asFlag);
  return (
    <button
      data-testid="client-record-payment"
      type="button"
      aria-pressed={open}
      onClick={() => setOpen(open ? undefined : true)}
    >
      {open ? 'Hide payment' : 'Record payment'}
    </button>
  );
}

export const contribution = ClientDetail.fill({ order: 34, Component: RecordPayment });
