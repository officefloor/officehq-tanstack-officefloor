import { defineSlot } from '../Slot';

/**
 * The home screen's body once a user is signed in and a sheet is loaded. Everything in the app is
 * kept per sheet, so each panel receives the active sheet id and the signed-in user.
 */
export const SheetPanels = defineSlot<{ sheet: string; user: string }>('sheet.panels');
