import { defineSlot } from '../Slot';

/**
 * Cells at the end of every invoice row — the region where an invoice's status and per-row actions
 * (e.g. "mark this invoice paid") live. Declared once here; features fill it without the table
 * listing what goes in it (CLAUDE.md rule 3). Its context is the invoice's id, its project's id
 * (for cache invalidation) and its current status.
 */
export const InvoiceRow = defineSlot<{ invoiceId: number; projectId: number; status: string }>(
  'invoice.row',
);
