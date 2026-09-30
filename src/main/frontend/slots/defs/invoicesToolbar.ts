import { defineSlot } from '../Slot';

/**
 * The project invoices toolbar — a region above a project's invoices list for controls that act on
 * it (sorting, and whatever comes later). InvoicesTable renders the region; features fill it
 * (CLAUDE.md rule 3), so a new control is a new `*.slot.tsx` file and the table is not edited again.
 * Its context is the project's id, so a control can key its URL/query state per project.
 */
export const InvoicesToolbar = defineSlot<{ projectId: number }>('invoices.toolbar');
