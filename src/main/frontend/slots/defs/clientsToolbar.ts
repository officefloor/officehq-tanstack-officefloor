import { defineSlot } from '../Slot';

/**
 * A toolbar over the clients list — controls that act on the whole list (a sort, a filter) rather
 * than one row. Features add controls here as new *.slot.tsx files; the list never lists them. Each
 * control owns a URL search key that the list reads, so nothing is passed between them. First use:
 * the control that sorts the list by name or by how much each client owes.
 */
export const ClientsToolbar = defineSlot('clients.toolbar');
