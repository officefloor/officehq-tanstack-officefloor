import { ClientDetail } from '../../slots/defs/clientDetail';
import { RecordPaymentPanel } from './RecordPaymentPanel';

// The record-payment form's presence on a client's detail page — its own file, filling the
// client-detail slot below the control that opens it. The detail route was written once and is not
// touched to add this; the panel queries for its own data (the client statement, scoped to the client
// it is handed) and only shows once the `payment` URL key opens it (owned by
// features/clients/recordPayment.slot.tsx).
export const contribution = ClientDetail.fill({
  order: 26,
  Component: RecordPaymentPanel,
});
