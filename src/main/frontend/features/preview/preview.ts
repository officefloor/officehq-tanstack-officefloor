import { asString, useSearchParam } from '../../url/useSearchParam';

/** The date this screen previews the sheet's today as (yyyy-MM-dd), or '' for none. It belongs to this user's screen
 *  (the `preview` search param), so loading another sheet keeps it, and no other user sees it. While it is set every
 *  figure on the screen takes it as the sheet's today, over the sheet's current records (see api/http and
 *  session#useSheetToday); every change still uses the sheet's real today. */
export const usePreview = () => useSearchParam('preview', asString);
