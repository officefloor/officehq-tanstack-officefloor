import { defineSlot } from '../Slot';

/**
 * Cells at the end of every invoice line-item row — the region where per-line actions (change this
 * line, remove this line) live. Declared once here; features fill it without the line-item table
 * listing what goes in it (CLAUDE.md rule 3). Its context is the line's id and current values plus
 * the invoice it belongs to (for cache invalidation on a write).
 */
export const LineItemRow = defineSlot<{
  invoiceId: number;
  lineItemId: number;
  description: string;
  qty: number;
  unitPrice: number;
}>('lineitem.row');
