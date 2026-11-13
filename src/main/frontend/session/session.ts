import { createContext, useContext } from 'react';
import { asString, useSearchParam } from '../url/useSearchParam';

// The signed-in name and the active sheet id live in the URL (`user`, `sheet`), so every feature
// reads them directly and each browser tab is its own session.
export const useCurrentUser = () => useSearchParam('user', asString);
export const useCurrentSheet = () => useSearchParam('sheet', asString);

// The date this screen shows the sheet as of (`asOf`, yyyy-MM-dd; '' for as it is now). It belongs to
// the screen, not the sheet: other users' screens are unaffected, and every change is refused while it is set.
export function useAsOf(): [string, (value: string | undefined) => void] {
  const [asOf, setAsOf] = useSearchParam('asOf', asString);
  return [useContext(LiveView) ? '' : asOf, setAsOf];
}

/** Within it, every read shows the sheet as it is now, whatever the screen's as-of date. */
export const LiveView = createContext(false);

/** The screen's as-of date read outside React (by the fetch surface), '' when none is set. */
export const currentAsOf = (): string => new URLSearchParams(window.location.search).get('asOf') ?? '';

// The screen's preview date (`preview`, yyyy-MM-dd; '' for none). Like the as-of date it belongs to the
// screen: while it is set every figure is read as if the sheet's today were that date, over the sheet's
// current records, and every change still uses the sheet's real today.
export const usePreview = () => useSearchParam('preview', asString);

/** The screen's preview date read outside React (by the fetch surface and query keys), '' when none is set. */
export const currentPreview = (): string => new URLSearchParams(window.location.search).get('preview') ?? '';
