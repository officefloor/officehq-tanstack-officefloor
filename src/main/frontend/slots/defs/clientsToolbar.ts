import { defineSlot } from '../Slot';

/**
 * Controls that act on the clients list — a filter, a toggle, a sort. Rendered above the clients
 * table by the clients feature; each control is its own `*.slot.tsx` file and owns a URL search
 * param, so the list never lists its controls. No context: it spans every client.
 */
export const ClientsToolbar = defineSlot('clients.toolbar');
