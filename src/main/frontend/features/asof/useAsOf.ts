import { asString, useSearchParam } from '../../url/useSearchParam';

/** The URL key holding the signed-in user's as-of date ("" or absent: none, the live sheet). It is this screen's
 *  alone: other users, and the store, are unaffected. */
export const AS_OF_PARAM = 'asof';

/** The as-of date the screen is shown at (YYYY-MM-DD), "" for none. While it is set every read is of the sheet as
 *  it stood at the end of that date, the date standing for the sheet's today, and every change is refused. */
export function useAsOf(): string {
  const [asOf] = useSearchParam(AS_OF_PARAM, asString);
  return asOf;
}

/** A read's path, asking for the sheet as of the date when one is set. */
export function readPath(path: string, asOf: string): string {
  return asOf === '' ? path : `${path}${path.includes('?') ? '&' : '?'}asOf=${encodeURIComponent(asOf)}`;
}
