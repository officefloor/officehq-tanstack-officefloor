import { defineSlot } from '../Slot';

/**
 * The body of the screen once a user is signed in and a sheet is loaded. Every per-sheet feature
 * fills it; its context is the active sheet id and the signed-in user.
 */
export const SheetBody = defineSlot<{ sheetId: string; user: string }>('sheet.body');
