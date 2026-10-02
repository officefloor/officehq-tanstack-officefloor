import { defineSlot } from '../Slot';

/**
 * The home dashboard's summary region — at-a-glance tiles about the business (overdue invoices, and
 * more over time). The dashboard page renders this region and never lists what fills it; each tile is
 * its own `*.slot.tsx` file under `features/dashboard/`.
 */
export const DashboardSummary = defineSlot('dashboard.summary');
