import { defineSlot } from '../Slot';

/**
 * The body of the home dashboard — the region where dashboard panels live (the top-clients panel,
 * and anything added later). Declared once here; features fill it without the page listing what goes
 * in it (CLAUDE.md rule 3). Its context is empty — the panels each query for themselves.
 */
export const Dashboard = defineSlot('dashboard.main');
