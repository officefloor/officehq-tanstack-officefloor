import { ClientDetail } from '../../slots/defs/clientDetail';
import { ClientStatementPanel } from './ClientStatementPanel';

// The statement's presence on a client's detail page — its own file, filling the client-detail slot
// below the open control. The detail route was written once and is not touched to add this; the
// panel queries for its own data (a ['invoices', 'statement', clientId] key) scoped to the client it
// is handed, and only shows once the `statement` URL key opens it.
export const contribution = ClientDetail.fill({
  order: 30,
  Component: ClientStatementPanel,
});
