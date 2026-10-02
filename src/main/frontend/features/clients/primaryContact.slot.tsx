import { ClientDetail } from '../../slots/defs/clientDetail';
import { PrimaryContact } from './PrimaryContact';

// The main-contact panel's presence on the client detail page — its own file, filling the shared
// client.detail region ahead of the contacts list. The detail route is not touched to add it
// (CLAUDE.md rule 3).
export const contribution = ClientDetail.fill({ order: 10, Component: PrimaryContact });
