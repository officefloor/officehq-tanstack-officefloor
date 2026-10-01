import { defineSlot } from '../Slot';

/**
 * An invoice's detail page. Features fill this region with panels about one invoice (its line items,
 * the total they add up to, a form to add another line). The detail route renders it with the
 * invoice id as context; the page never lists what goes in it, so each panel is a new *.slot.tsx file.
 */
export const InvoiceDetail = defineSlot<{ invoiceId: number }>('invoice.detail');
