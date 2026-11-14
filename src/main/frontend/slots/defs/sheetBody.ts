import { defineSlot } from '../Slot';

/**
 * The loaded sheet's content. Everything kept per sheet contributes here and is handed the active
 * sheet's id and the signed-in user; its queries go under ['sheet', sheetId, ...].
 */
export const SheetBody = defineSlot<{ sheetId: string; user: string }>('sheet.body');
