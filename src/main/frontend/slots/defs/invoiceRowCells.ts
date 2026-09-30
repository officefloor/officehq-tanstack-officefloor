import { defineSlot } from '../Slot';

/**
 * The per-row cell region of the project invoices table. Each invoice row renders this with its own
 * id; features fill it with extra `<td>` cells (e.g. the amount still due after payments) without the
 * list ever listing what cells it grows. Mirrors {@link ProjectRowActions}: the context is the
 * invoice's id, so a contribution can scope its own data to that invoice.
 */
export const InvoiceRowCells = defineSlot<{ invoiceId: number }>('invoice.row.cells');
