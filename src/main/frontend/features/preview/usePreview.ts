import { asString, useSearchParam } from '../../url/useSearchParam';

/** The URL key holding the signed-in user's preview date ("" or absent: none). It is this screen's alone: other
 *  users, and the store, are unaffected. */
export const PREVIEW_PARAM = 'preview';

/** The preview date (YYYY-MM-DD), "" for none. While it is set every figure on screen is worked as if the sheet's
 *  today were that date, over the sheet's current records; every change still uses the sheet's real today. */
export function usePreview(): string {
  const [preview] = useSearchParam(PREVIEW_PARAM, asString);
  return preview;
}
