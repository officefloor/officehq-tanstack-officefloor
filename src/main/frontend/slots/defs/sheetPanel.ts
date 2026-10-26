import { defineSlot } from '../Slot';

/**
 * The body of the loaded sheet. Every feature kept per sheet contributes its own panel here; it
 * is rendered only once a sheet has loaded, with the signed-in user and that sheet's id.
 */
export const SheetPanel = defineSlot<{ user: string; sheetId: string }>('sheet.panel');
