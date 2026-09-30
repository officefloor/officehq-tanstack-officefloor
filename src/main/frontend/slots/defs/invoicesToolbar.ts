import { defineSlot } from '../Slot';

/**
 * The project invoices toolbar: the strip above the invoices table. The invoices panel renders it
 * once; features (the sort-by-due-date control, and anything added later) fill it with
 * `InvoicesToolbar.fill(...)`. Adding a control here is a new file, never an edit to the panel.
 */
export const InvoicesToolbar = defineSlot('project.invoices.toolbar');
