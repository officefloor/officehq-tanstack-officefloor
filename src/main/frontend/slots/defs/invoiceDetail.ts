import { defineSlot } from '../Slot';

/**
 * An invoice's detail page — panels about one invoice (its recorded payments, and more over time).
 * The detail route renders this region and never lists what fills it; each panel is its own
 * `*.slot.tsx` file under `features/invoices/`. Handed the invoice id as context.
 */
export const InvoiceDetail = defineSlot<{ invoiceId: number }>('invoice.detail');
