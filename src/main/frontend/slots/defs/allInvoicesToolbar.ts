import { defineSlot } from '../Slot';

/**
 * The all-invoices toolbar: the strip above the one-place invoices list. The all-invoices page
 * renders it once; features (the status filter, and anything added later) fill it with
 * `AllInvoicesToolbar.fill(...)`. Adding a control here is a new file, never an edit to the page.
 */
export const AllInvoicesToolbar = defineSlot('invoices.all.toolbar');
