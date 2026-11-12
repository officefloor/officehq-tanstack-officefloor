import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** `today` (yyyy-MM-dd) is the sheet's own date: every date decision and default uses it, never the clock.
 *  An `archived` sheet stays visible but the server refuses every change to it. */
export type Sheet = { id: string; today: string; archived: boolean };

/** Every per-sheet query key starts with ['sheet', id], so loading a sheet re-reads all of it. */
export const sheetKey = (id: string) => ['sheet', id] as const;

export function useSheet(id: string) {
  return useQuery({
    queryKey: sheetKey(id),
    queryFn: () => getJson<Sheet>(`/api/sheets/${encodeURIComponent(id)}`),
    enabled: id !== '',
  });
}

/** The sheet's today (yyyy-MM-dd), or undefined while the sheet loads. Use it for every default date. */
export function useSheetToday(id: string): string | undefined {
  return useSheet(id).data?.today;
}
