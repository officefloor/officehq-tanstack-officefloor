import { ClientDetail } from '../../slots/defs/clientDetail';
import { PrimaryContactPanel } from './PrimaryContactPanel';

// The client's main-contact panel on the client-detail page — its own file, filling the client-detail
// slot above the contacts table. The detail route is not touched to add this; the panel queries for
// its own data (['contacts']) scoped to the client it is handed.
export const contribution = ClientDetail.fill({
  order: 15,
  Component: PrimaryContactPanel,
});
