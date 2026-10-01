import { defineSlot } from '../Slot';

/**
 * A row action on the clients list — a per-client control rendered in the row's last cell (the
 * Slot is a Fragment, so it is valid inside a <td>). Features add actions here as new *.slot.tsx
 * files; the list never lists them. First use: the link that opens a client's detail page.
 */
export const ClientRow = defineSlot<{ clientId: number }>('client.row');
