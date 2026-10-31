import { asString, useSearchParam } from '../../url/useSearchParam';

/**
 * The date this user's screen shows the sheet as of (yyyy-mm-dd), or '' for the sheet as it stands.
 * It lives in the URL (`asof`), so it is this screen's alone. While it is set every read is of the
 * sheet as it stood at the end of that date, and every change is refused (see api/http.ts).
 */
export function useAsOf() {
  return useSearchParam('asof', asString);
}

/** A read's query key: the live key, or one apart from it for the sheet as of a date. It keeps the
 *  live key as its prefix, so invalidating the live key re-reads both. */
export function asOfKey<K extends readonly unknown[]>(key: K, asOf: string): K {
  return (asOf === '' ? key : [...key, { asOf }]) as unknown as K;
}

/** A read's path, asking for the sheet as of the date when one is set. */
export function asOfPath(path: string, asOf: string): string {
  return asOf === '' ? path : `${path}${path.includes('?') ? '&' : '?'}asOf=${encodeURIComponent(asOf)}`;
}
