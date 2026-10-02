import { ClientDetail } from '../../slots/defs/clientDetail';
import { ClientCurrency } from './ClientCurrency';

// The currency panel's presence on the client detail page — its own file filling the shared
// client.detail region (CLAUDE.md rule 3). Ordered before the counts/contacts panels so the currency
// control sits near the top. The detail route is not touched to add it.
export const contribution = ClientDetail.fill({ order: 5, Component: ClientCurrency });
