import { ClientDetail } from '../../slots/defs/clientDetail';
import { ClientProjectsPanel } from './ClientProjectsPanel';

// The projects' presence on a client's detail page — its own file, filling the client-detail slot.
// The detail route was written once and is not touched to add this; the panel queries for its own
// data (['projects']) scoped to the client it is handed.
export const contribution = ClientDetail.fill({
  order: 10,
  Component: ClientProjectsPanel,
});
