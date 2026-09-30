import { defineSlot } from '../Slot';

/**
 * The clients page toolbar: the strip above the clients table. The page renders it once; features
 * (the name search box, and anything added later) fill it with `ClientsToolbar.fill(...)`. Adding a
 * control here is a new file, never an edit to the page.
 */
export const ClientsToolbar = defineSlot('clients.toolbar');
