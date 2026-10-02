import { DashboardSummary } from '../../slots/defs/dashboardSummary';
import { DashboardTopClients } from './DashboardTopClients';

// The top-clients tile's presence on the home dashboard — its own file filling the shared
// dashboard.summary region (CLAUDE.md rule 3). The dashboard page is not touched to add it.
export const contribution = DashboardSummary.fill({ order: 20, Component: DashboardTopClients });
