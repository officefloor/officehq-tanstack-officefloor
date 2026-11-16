import { defineSlot } from '../Slot';

/**
 * The loaded sheet's content on the home screen. Rendered once a user is signed in and a sheet is
 * loaded; everything a contribution shows or changes belongs to `sheetId` and is done as `user`.
 */
export const SheetPage = defineSlot<{ sheetId: string; user: string }>('sheet.page');
