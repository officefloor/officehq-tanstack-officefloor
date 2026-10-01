import { defineSlot } from '../Slot';

/**
 * Controls that act on a project's invoice list — a sort, a filter, an export. Rendered above the
 * list by the invoices feature; each control is its own `*.slot.tsx` file and owns a URL search
 * param, so the list never lists its controls. Handed the project id as context.
 */
export const InvoiceListToolbar = defineSlot<{ projectId: number }>('invoices.toolbar');
