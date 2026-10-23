import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';

/** A sheet: its id, its today (ISO yyyy-mm-dd) and whether it is archived (every change refused). */
export type Sheet = { id: string; today: string; archived: boolean };

export const sheetKey = (sheetId: string) => ['sheet', sheetId, 'meta'] as const;

export function useSheet(sheetId: string) {
  return useQuery({
    queryKey: sheetKey(sheetId),
    queryFn: () => getJson<Sheet>(`/api/sheets/${encodeURIComponent(sheetId)}`),
  });
}

/** The date this user's screen shows the sheet as of (yyyy-mm-dd; '' when it shows it as it is now). It belongs to
 *  the screen, not the sheet: other users are unaffected. While it is set, everything under the sheet is read as it
 *  stood at the end of that day, and every change is refused. */
export function useAsOf() {
  return useSearchParam('asOf', asString);
}

/** The date this user's screen previews the sheet's today as (yyyy-mm-dd; '' when none). It belongs to the screen,
 *  not the sheet: other users are unaffected. While it is set, every figure is shown over the sheet's current records
 *  as if its today were that date; every change is still made on the sheet's real today. */
export function usePreview() {
  return useSearchParam('preview', asString);
}

/**
 * The sheet's today as the screen figures it — the date every date-dependent figure on the sheet
 * uses. Never read the real clock (`new Date()`) for these; use this instead. '' while loading.
 * While the screen shows the sheet as of a date, that date stands in for it; else while it
 * previews a date, that one does. A default date a change is made on uses useChangeToday.
 */
export function useSheetToday(sheetId: string): string {
  const [asOf] = useAsOf();
  const [preview] = usePreview();
  const today = useChangeToday(sheetId);
  return asOf === '' && preview !== '' && today !== '' ? preview : today;
}

/**
 * The sheet's today a change is made on — every default date of a change (a form's blank date,
 * a from-date left as today). A preview date never stands in for it. '' while loading.
 */
export function useChangeToday(sheetId: string): string {
  const [asOf] = useAsOf();
  const today = useSheet(sheetId).data?.today ?? '';
  return asOf !== '' && today !== '' ? asOf : today;
}
