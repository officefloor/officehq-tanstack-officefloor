import { ClientDetail } from '../../slots/defs/clientDetail';
import { ClientPrintStatement } from './ClientPrintStatement';

// The printable statement summary's presence on a client's detail page — its own file, filling the
// client-detail slot below the statement panel (order 40, after statement's 30). The detail route is
// not touched to add this; the panel queries for its own data scoped to the client it is handed and
// only shows once the `statement` URL key opens it.
export const contribution = ClientDetail.fill({
  order: 40,
  Component: ClientPrintStatement,
});
