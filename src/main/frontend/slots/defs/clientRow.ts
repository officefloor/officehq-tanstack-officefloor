import { defineSlot } from '../Slot';
import type { Client } from '../../features/clients/clients';

/**
 * The actions cell of one client row in the clients list — per-row controls that act on a single
 * client (archiving it, and more over time). The clients table renders this region once per row and
 * never lists what fills it; each action is its own `*.slot.tsx` file under `features/clients/`.
 * Handed the whole client as context, so an action has its id and name to work with.
 */
export const ClientRow = defineSlot<{ client: Client }>('client.row');
