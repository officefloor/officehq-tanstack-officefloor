import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

export type SheetToday = { sheet: string; today: string };

export const sheetTodayKey = (sheet: string) => ['sheet', sheet, 'today'] as const;

/** The sheet's today (YYYY-MM-DD). Every date decision and default date on screen reads this —
 *  never `new Date()`. Undefined while loading. */
export function useSheetToday(sheet: string): string | undefined {
  const { data } = useQuery({
    queryKey: sheetTodayKey(sheet),
    queryFn: () => getJson<SheetToday>(`/api/sheets/${encodeURIComponent(sheet)}/today`),
    enabled: sheet !== '',
  });
  return data?.today;
}
