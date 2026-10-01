import { defineSlot } from '../Slot';

/**
 * Controls that act on the all-invoices list — a filter, a sort, an export. Rendered above the
 * cross-project invoice list by the invoices feature; each control is its own `*.slot.tsx` file and
 * owns a URL search param, so the list never lists its controls. No context: it spans every project.
 */
export const AllInvoicesToolbar = defineSlot('allInvoices.toolbar');
