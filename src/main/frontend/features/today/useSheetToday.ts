import { useQuery } from '@tanstack/react-query';
import { getJsonUnpreviewed } from '../../api/http';
import { useReader } from '../../api/readNow';

export type SheetToday = { id: string; today: string; archived: boolean };

/** The sheet's today (yyyy-mm-dd), and whether it is archived. Every date decision and default date in a sheet uses this,
 *  never the browser's clock. While this screen previews the sheet at a date, that date is its today here. */
export function useSheetToday(sheetId: string) {
  const read = useReader();
  return useQuery({
    queryKey: read.key(['sheet', sheetId, 'today']),
    queryFn: () => read.get<SheetToday>(`/api/sheets/${encodeURIComponent(sheetId)}/today`),
  });
}

/** The sheet's real today, and whether it is archived, whatever date this screen previews it at. */
export function useRealSheetToday(sheetId: string) {
  return useQuery({
    queryKey: ['sheet', sheetId, 'today', 'real'],
    queryFn: () => getJsonUnpreviewed<SheetToday>(`/api/sheets/${encodeURIComponent(sheetId)}/today`),
  });
}
