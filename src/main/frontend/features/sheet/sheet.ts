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

/**
 * The sheet's today — the date every date-dependent decision and every default date on the sheet
 * uses. Never read the real clock (`new Date()`) for these; use this instead. '' while loading.
 * While the screen shows the sheet as of a date, that date stands in for it.
 */
export function useSheetToday(sheetId: string): string {
  const [asOf] = useAsOf();
  const today = useSheet(sheetId).data?.today ?? '';
  return asOf !== '' && today !== '' ? asOf : today;
}
