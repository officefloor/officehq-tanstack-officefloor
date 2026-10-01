import { defineSlot } from '../Slot';

/**
 * A row action on a project's invoices table — a per-invoice control rendered in the row's last cell
 * (the Slot is a Fragment, so it is valid inside a <td>). Features add actions here as new *.slot.tsx
 * files; the invoices panel never lists them. First use: the button that marks an invoice paid.
 */
export const InvoiceRow = defineSlot<{ invoiceId: number; projectId: number }>('invoice.row');
