import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

export type Sheet = { id: string; today: string; archived: boolean };

export const sheetKey = (sheetId: string) => ['sheet', sheetId, 'info'] as const;

/** The loaded sheet: its id, its today (yyyy-MM-dd) and whether a steward has archived it. */
export function useSheet(sheetId: string) {
  return useQuery({
    queryKey: sheetKey(sheetId),
    queryFn: () => getJson<Sheet>(`/api/sheets/${encodeURIComponent(sheetId)}`),
  });
}

/**
 * The sheet's today. Every date-dependent decision and every default date on the screen uses this,
 * never the real clock (`new Date()`); '' until the sheet has been read.
 */
export function useSheetToday(sheetId: string): string {
  return useSheet(sheetId).data?.today ?? '';
}
