import { defineSlot } from '../Slot';

/**
 * The per-row action region of the clients table. Each client row renders this with its own id;
 * features fill it (e.g. the link that opens the client) without the list ever listing the actions.
 */
export const ClientRowActions = defineSlot<{ clientId: number }>('client.row.actions');
