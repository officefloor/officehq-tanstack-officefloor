import { defineSlot } from '../Slot';

/**
 * The body of the active sheet, rendered once a user is signed in and a sheet is loaded. A
 * feature that shows or changes a sheet's data fills this region. Its data lives under query
 * keys starting ['sheet', sheetId, ...]: loading a sheet drops every such key, so the whole
 * sheet is re-read from the store.
 */
export const SheetBody = defineSlot<{ sheetId: string; user: string }>('sheet.body');
