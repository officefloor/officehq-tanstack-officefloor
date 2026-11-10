import { defineSlot } from '../Slot';

/**
 * The loaded sheet's body on the home screen. Everything kept per sheet (its people, claims,
 * settings) contributes here; it renders only once a user is signed in and a sheet is loaded, and
 * is remounted whenever a sheet is (re-)loaded, so a contribution starts from a fresh read.
 * Server data for a sheet lives under the query key ['sheet', sheetId, ...].
 */
export const SheetBody = defineSlot<{ sheetId: string; user: string }>('sheet.body');
