import { ClientDetail } from '../../slots/defs/clientDetail';
import { ClientContacts } from './ClientContacts';

// The contacts panel's presence on the client detail page — its own file, filling the shared
// client.detail region. The detail route is not touched to add it (CLAUDE.md rule 3).
export const contribution = ClientDetail.fill({ order: 20, Component: ClientContacts });
