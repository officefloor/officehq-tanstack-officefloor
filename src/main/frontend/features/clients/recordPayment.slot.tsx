import { ClientDetail } from '../../slots/defs/clientDetail';
import { asString, useSearchParam } from '../../url/useSearchParam';

// The control that opens the "record a payment" form on a client's detail page — its own file,
// filling the client-detail slot. It OWNS the `payment` URL key (rule 4: whether the form is open
// outlives a click, so it lives in the URL, not useState). The form panel
// (features/clients/recordPaymentForm.slot.tsx) reads the same key to decide whether to show; nothing
// is passed between them. Carries data-testid="client-record-payment" (the test contract).
function RecordPaymentOpen() {
  const [payment, setPayment] = useSearchParam('payment', asString);
  const open = payment === 'open';
  return (
    <button
      type="button"
      data-testid="client-record-payment"
      aria-pressed={open}
      onClick={() => setPayment(open ? undefined : 'open')}
    >
      {open ? 'Hide payment' : 'Record payment'}
    </button>
  );
}

export const contribution = ClientDetail.fill({ order: 25, Component: RecordPaymentOpen });
