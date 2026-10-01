import { ClientDetail } from '../../slots/defs/clientDetail';
import { ClientCounts } from './ClientCounts';

// The counts summary's presence on the client detail page — its own file, filling the shared
// client.detail region. Ordered before the projects/contacts panels so the at-a-glance numbers sit
// at the top. The detail route is not touched to add it (CLAUDE.md rule 3).
export const contribution = ClientDetail.fill({ order: 10, Component: ClientCounts });
