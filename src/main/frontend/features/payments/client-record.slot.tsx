import { ClientDetail } from '../../slots/defs/clientDetail';
import { useSearchParam, asFlag } from '../../url/useSearchParam';
import { CLIENT_PAYMENT_PARAM } from './queries';
import { ClientPaymentForm } from './ClientPaymentForm';

// The "record a payment" panel on a client's detail page — one new *.slot.tsx file filling the
// ClientDetail region (CLAUDE.md rule 3). Nothing existing is edited to add it. The control that
// reveals the split-payment form owns the `recordPayment` URL key (rule 4): whether the form is open
// outlives a click, so it lives in the URL, and the panel reads the same key to decide whether to
// render the form.
function ClientRecordPaymentPanel({ clientId }: { clientId: number }) {
  const [open, setOpen] = useSearchParam(CLIENT_PAYMENT_PARAM, asFlag);
  return (
    <section data-testid="client-payment">
      <button
        type="button"
        data-testid="client-record-payment"
        onClick={() => setOpen(open ? undefined : true)}
      >
        Record payment
      </button>
      {open ? <ClientPaymentForm clientId={clientId} /> : null}
    </section>
  );
}

export const contribution = ClientDetail.fill({ order: 30, Component: ClientRecordPaymentPanel });
