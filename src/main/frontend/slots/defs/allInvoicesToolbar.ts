import { defineSlot } from '../Slot';

/**
 * The all-invoices toolbar — a region above the cross-project invoices list for controls that act on
 * it (filtering by stage, and whatever comes later). AllInvoicesTable renders the region; features
 * fill it (CLAUDE.md rule 3), so a new control is a new `*.slot.tsx` file and the table is not
 * edited again to add one.
 */
export const AllInvoicesToolbar = defineSlot('invoices.all.toolbar');
