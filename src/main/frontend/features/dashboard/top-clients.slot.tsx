import { Dashboard } from '../../slots/defs/dashboard';
import { TopClients } from './TopClients';

// The top-clients panel on the home dashboard — one new *.slot.tsx file filling the dashboard.main
// region (CLAUDE.md rule 3). Nothing existing is edited to add it; the panel queries for itself.
export const contribution = Dashboard.fill({ order: 20, Component: TopClients });
