import { DashboardSummary } from '../../slots/defs/dashboardSummary';
import { DashboardOverdue } from './DashboardOverdue';

// The overdue-invoices tile's presence on the home dashboard — its own file filling the shared
// dashboard.summary region (CLAUDE.md rule 3). The dashboard page is not touched to add it.
export const contribution = DashboardSummary.fill({ order: 10, Component: DashboardOverdue });
