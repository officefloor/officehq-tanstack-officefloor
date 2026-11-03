import { createContext, useContext } from 'react';
import { getJson, getJsonNow } from './http';

/** Set to true around components that must show the sheet as the store holds it now while the screen shows it as
 *  of a date: the shared read hooks then answer as of now, under keys of their own. */
export const ReadNow = createContext(false);

/** How a shared read hook reads: its getter, and its query key (suffixed when it reads now under an as-of date). */
export function useReader(): { get: <T>(path: string) => Promise<T>; key: (key: unknown[]) => unknown[] } {
  const now = useContext(ReadNow);
  return now
    ? { get: getJsonNow, key: (key) => [...key, 'read-now'] }
    : { get: getJson, key: (key) => key };
}
