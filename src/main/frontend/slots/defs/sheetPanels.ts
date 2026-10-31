import { defineSlot } from '../Slot';

/**
 * The active sheet's content on the home screen. Rendered only once someone is signed in and a
 * sheet is loaded; everything kept per sheet contributes a panel here, keyed by `sheet`.
 */
export const SheetPanels = defineSlot<{ user: string; sheet: string }>('sheet.panels');
