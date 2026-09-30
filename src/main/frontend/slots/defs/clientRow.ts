import { defineSlot } from '../Slot';

/**
 * A cell at the end of every client row — the region where per-row actions live (e.g. "open this
 * client"). Declared once here; features fill it without the table listing what goes in it
 * (CLAUDE.md rule 3). Its context is the row's client id.
 */
export const ClientRow = defineSlot<{ clientId: number }>('client.row');
