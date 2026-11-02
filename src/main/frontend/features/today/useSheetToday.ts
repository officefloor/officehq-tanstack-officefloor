import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

export type SheetToday = { id: string; today: string };

/** The sheet's today (yyyy-mm-dd). Every date decision and default date in a sheet uses this,
 *  never the browser's clock. */
export function useSheetToday(sheetId: string) {
  return useQuery({
    queryKey: ['sheet', sheetId, 'today'],
    queryFn: () => getJson<SheetToday>(`/api/sheets/${encodeURIComponent(sheetId)}/today`),
  });
}
