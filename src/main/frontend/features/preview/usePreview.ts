import { asString, useSearchParam } from '../../url/useSearchParam';

/**
 * The date this user's screen previews the sheet as of (yyyy-mm-dd), or '' for none. It lives in the
 * URL (`preview`), so it is this screen's alone. While it is set every read is of the sheet's current
 * records with that date standing in for the sheet's today; every change still goes to the sheet as
 * it is, on its real today (see features/asof/useAsOf.ts `useView`).
 */
export function usePreview(): [string, (value: string | undefined) => void] {
  const [preview, setPreview] = useSearchParam('preview', asString);
  return [preview, setPreview];
}
