import { defineSlot } from '../Slot';
import type { Invoice } from '../../features/invoices/invoices';

/**
 * A cell on one invoice row in a project's invoice list — per-row figures and controls that read a
 * single invoice (what is still left to pay on it, and more over time). The invoice table renders
 * this region once per row and never lists what fills it; each contribution is its own `*.slot.tsx`
 * file under `features/invoices/`. Handed the whole invoice as context, so a contribution has its id
 * and amount to work with.
 */
export const InvoiceRow = defineSlot<{ invoice: Invoice }>('invoice.row');
