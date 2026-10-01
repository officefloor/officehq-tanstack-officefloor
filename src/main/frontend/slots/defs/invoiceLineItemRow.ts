import { defineSlot } from '../Slot';
import type { LineItem } from '../../features/invoices/lineItems';

/**
 * The actions cell of one line-item row on an invoice — per-row controls that act on a single charge
 * line (removing it, and more over time). The line-item table renders this region once per row and
 * never lists what fills it; each action is its own `*.slot.tsx` file under `features/invoices/`.
 * Handed the whole line item as context, so an action has its id and invoice to work with.
 */
export const InvoiceLineItemRow = defineSlot<{ lineItem: LineItem }>('invoice.lineItem.row');
