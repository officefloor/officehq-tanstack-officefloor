import { ClientDetail } from '../../slots/defs/clientDetail';
import { ClientStatement } from './ClientStatement';

// The statement's presence on the client detail page — its own file filling the shared client.detail
// region (CLAUDE.md rule 3). The detail route is not touched to add it.
export const contribution = ClientDetail.fill({ order: 20, Component: ClientStatement });
