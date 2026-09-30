import { DashboardPanels } from '../../slots/defs/dashboardPanels';
import { TopClientsPanel } from './TopClientsPanel';

// The top-clients panel's presence on the home screen — its own file, filling the dashboard-panels
// slot below the summary. The dashboard was written once and is not touched to add this; the panel
// queries for its own data (a ['dashboard', 'top-clients'] key).
export const contribution = DashboardPanels.fill({
  order: 10,
  Component: TopClientsPanel,
});
