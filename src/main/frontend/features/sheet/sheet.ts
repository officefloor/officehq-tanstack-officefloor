import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** `lastBatch`: the number of the sheet's last bulk settle batch, null when there has been none. */
export type Sheet = { id: string; today: string; archived: boolean; lastBatch: number | null };

export const sheetKey = (sheet: string) => ['sheet', sheet] as const;

/** The sheet's settings, its today among them. Every default date and date decision on screen
 *  reads `today` from here, never from the browser's clock. */
export function useSheet(sheet: string) {
  return useQuery({
    queryKey: sheetKey(sheet),
    queryFn: () => getJson<Sheet>(`/api/sheets/${encodeURIComponent(sheet)}`),
  });
}

/** The sheet's today (yyyy-MM-dd), or undefined while it loads. */
export function useSheetToday(sheet: string): string | undefined {
  return useSheet(sheet).data?.today;
}
