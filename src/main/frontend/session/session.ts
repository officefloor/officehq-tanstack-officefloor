import { asString, useSearchParam } from '../url/useSearchParam';

// The signed-in name and the active sheet id live in the URL (`user`, `sheet`), so every feature
// reads them directly and each browser tab is its own session.
export const useCurrentUser = () => useSearchParam('user', asString);
export const useCurrentSheet = () => useSearchParam('sheet', asString);

// The date this screen shows the sheet as of (`asOf`, yyyy-MM-dd; '' for as it is now). It belongs to
// the screen, not the sheet: other users' screens are unaffected, and every change is refused while it is set.
export const useAsOf = () => useSearchParam('asOf', asString);

/** The screen's as-of date read outside React (by the fetch surface), '' when none is set. */
export const currentAsOf = (): string => new URLSearchParams(window.location.search).get('asOf') ?? '';
