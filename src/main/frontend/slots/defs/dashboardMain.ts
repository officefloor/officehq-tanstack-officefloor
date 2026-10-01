import { defineSlot } from '../Slot';

/**
 * The dashboard page's main region, below the summary figures. Features fill it with whatever
 * belongs on the home screen (a ranked panel, a tile); the page never lists what goes in it, so each
 * addition is a new *.slot.tsx. Rendered by routes/dashboard.tsx.
 */
export const DashboardMain = defineSlot('dashboard.main');
