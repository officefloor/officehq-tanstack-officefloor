import { defineSlot } from '../Slot';

/**
 * The body of an invoice's detail page — the region where per-invoice panels live (the payments
 * list, and anything added later). Declared once here; features fill it without the page listing
 * what goes in it (CLAUDE.md rule 3). Its context is the invoice's id.
 */
export const InvoiceDetail = defineSlot<{ invoiceId: number }>('invoice.detail');
