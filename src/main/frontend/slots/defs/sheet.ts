import { defineSlot } from '../Slot';

/**
 * The loaded sheet's body. Every feature kept per sheet contributes here; the home page renders it
 * only once someone is signed in and a sheet is loaded. Server data under a sheet is keyed
 * ['sheet', sheetId, ...] so a sheet load re-reads all of it.
 */
export const SheetBody = defineSlot<{ sheetId: string; user: string }>('sheet.body');
