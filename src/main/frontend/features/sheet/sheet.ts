import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A sheet: its id and its today (ISO yyyy-mm-dd). */
export type Sheet = { id: string; today: string };

export const sheetKey = (sheetId: string) => ['sheet', sheetId, 'meta'] as const;

export function useSheet(sheetId: string) {
  return useQuery({
    queryKey: sheetKey(sheetId),
    queryFn: () => getJson<Sheet>(`/api/sheets/${encodeURIComponent(sheetId)}`),
  });
}

/**
 * The sheet's today — the date every date-dependent decision and every default date on the sheet
 * uses. Never read the real clock (`new Date()`) for these; use this instead. '' while loading.
 */
export function useSheetToday(sheetId: string): string {
  return useSheet(sheetId).data?.today ?? '';
}
