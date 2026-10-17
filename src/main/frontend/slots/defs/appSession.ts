import { defineSlot } from '../Slot';

/**
 * The shell's session bar: who is signed in and which sheet is active. Filled by
 * features/session/ — the sign-in form, then the sheet picker once someone is signed in.
 */
export const AppSession = defineSlot('app.session');
