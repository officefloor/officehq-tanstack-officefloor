import { useQuery } from '@tanstack/react-query';
import { useReader } from '../../api/readNow';

export type SheetToday = { id: string; today: string; archived: boolean };

/** The sheet's today (yyyy-mm-dd), and whether it is archived. Every date decision and default date in a sheet uses this,
 *  never the browser's clock. */
export function useSheetToday(sheetId: string) {
  const read = useReader();
  return useQuery({
    queryKey: read.key(['sheet', sheetId, 'today']),
    queryFn: () => read.get<SheetToday>(`/api/sheets/${encodeURIComponent(sheetId)}/today`),
  });
}
