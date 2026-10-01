import { defineSlot } from '../Slot';

/**
 * The toolbar above the all-invoices list. Features fill this region with controls that narrow or
 * reorder the one cross-project list — a status filter, and whatever controls come later. The
 * invoices page renders it; the page never lists what goes in it, so each control is a new
 * *.slot.tsx file that owns its own URL key.
 */
export const AllInvoicesToolbar = defineSlot('invoices.toolbar');
