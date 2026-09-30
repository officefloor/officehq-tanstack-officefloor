import { defineSlot } from '../Slot';

/**
 * An invoice's detail region. The invoice-detail route renders this once; features (its line items,
 * and anything added later) fill it with `InvoiceDetail.fill(...)`, each panel querying for itself.
 * The context is the invoice's id, so a contribution can scope its own data to that invoice.
 */
export const InvoiceDetail = defineSlot<{ invoiceId: number }>('invoice.detail');
