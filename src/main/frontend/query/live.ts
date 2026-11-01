import { createContext, useContext } from 'react';
import { getJson, getJsonNow } from '../api/http';
import { asString, useSearchParam } from '../url/useSearchParam';

/** Set around a part of the screen that shows the sheets as they are now even while the screen shows the sheet as
 *  of a date (the consolidated figures). */
export const LiveFigures = createContext(false);

/** The query key and the read a sheet query uses: its own, or, inside {@link LiveFigures} while an as-of date is
 *  set, a key of its own ('live' first) read as the sheet is now. With no as-of date the two are the same query. */
export function useReadScope<K extends readonly unknown[]>(key: K): { key: readonly unknown[]; get: typeof getJson } {
  const live = useContext(LiveFigures);
  const [asof] = useSearchParam('asof', asString);
  return live && asof ? { key: ['live', ...key], get: getJsonNow } : { key, get: getJson };
}
