import { asString, useSearchParam } from '../../url/useSearchParam';
import { usePreview } from '../preview/usePreview';

/**
 * The date this user's screen shows the sheet as of (yyyy-mm-dd), or '' for the sheet as it stands.
 * It lives in the URL (`asof`), so it is this screen's alone. While it is set every read is of the
 * sheet as it stood at the end of that date, and every change is refused (see api/http.ts).
 */
export function useAsOf(): [string, (value: string | undefined) => void] {
  const [asOf, setAsOf] = useSearchParam('asof', asString);
  return [asOf, setAsOf];
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

/** What this screen's reads are of: the sheet as of a date (`asOf`), and/or with a preview date
 *  standing in for the sheet's today (`preview`, see features/preview/usePreview.ts); '' for none. */
export type View = { asOf: string; preview: string };

export function useView(): View {
  const [asOf] = useAsOf();
  const [preview] = usePreview();
  return { asOf, preview };
}

/** A read's query key for the view: the live key, or one apart from it. It keeps the live key as its
 *  prefix, so invalidating the live key re-reads every view. */
export function viewKey<K extends readonly unknown[]>(key: K, view: View): K {
  if (view.asOf === '' && view.preview === '') return key;
  return [...key, { asOf: view.asOf, preview: view.preview }] as unknown as K;
}

/** A read's path, asking for the view when one is set. */
export function viewPath(path: string, view: View): string {
  const withAsOf = asOfPath(path, view.asOf);
  return view.preview === ''
    ? withAsOf
    : `${withAsOf}${withAsOf.includes('?') ? '&' : '?'}preview=${encodeURIComponent(view.preview)}`;
}
