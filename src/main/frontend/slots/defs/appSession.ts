import { defineSlot } from '../Slot';

/**
 * The shell's session bar: who is signed in and which sheet is active. Its contributions own the
 * `user` and `sheet` search params (read anywhere with url/useSearchParam).
 */
export const AppSession = defineSlot('app.session');
