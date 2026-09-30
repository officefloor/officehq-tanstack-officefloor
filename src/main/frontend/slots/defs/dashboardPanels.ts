import { defineSlot } from '../Slot';

/**
 * The home screen's panels region. The dashboard route renders this once, below its summary; features
 * (the top-clients panel, and anything added later) fill it with `DashboardPanels.fill(...)`, each
 * panel querying for its own data. Context-free — a dashboard panel scopes to the whole app.
 */
export const DashboardPanels = defineSlot('dashboard.panels');
