import { ClientDetail } from '../../slots/defs/clientDetail';
import { useSearchParam, asFlag } from '../../url/useSearchParam';
import { ClientPaymentForm } from './ClientPaymentForm';

// "Record a payment" on the client detail page — its own file filling the shared client.detail region
// (CLAUDE.md rule 3); the detail route is not touched to add it. Whether the split-payment form is
// open lives in the URL (`recordPayment` key, CLAUDE.md rule 4), so it outlives the click and the
// control that opens it owns the key.
function RecordPayment({ clientId }: { clientId: number }) {
  const [open, setOpen] = useSearchParam('recordPayment', asFlag);
  return (
    <section data-testid="client-payment">
      <h2>Payment</h2>
      <button type="button" data-testid="client-record-payment" onClick={() => setOpen(true)}>
        Record payment
      </button>
      {open && <ClientPaymentForm clientId={clientId} />}
    </section>
  );
}

export const contribution = ClientDetail.fill({ order: 30, Component: RecordPayment });
