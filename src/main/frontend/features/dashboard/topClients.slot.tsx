import { DashboardMain } from '../../slots/defs/dashboardMain';
import { TopClients } from './TopClients';

// The "top clients" panel's presence on the dashboard — one new file filling the dashboard.main
// region, the page is not touched to put it there (see features/home/nav.slot.tsx for the worked
// example of the mechanism).
export const contribution = DashboardMain.fill({ order: 10, Component: TopClients });
