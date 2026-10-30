import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { readPath, useAsOf } from '../asof/useAsOf';

export type SheetToday = { sheet: string; today: string; archived: boolean };

export const sheetTodayKey = (sheet: string, asOf = '') => ['sheet', sheet, 'today', asOf] as const;

/** The sheet's today (YYYY-MM-DD; the as-of date while one is set). Every date decision and default date on screen reads this —
 *  never `new Date()`. Undefined while loading. */
export function useSheetToday(sheet: string): string | undefined {
  const asOf = useAsOf();
  const { data } = useQuery({
    queryKey: sheetTodayKey(sheet, asOf),
    queryFn: () => getJson<SheetToday>(readPath(`/api/sheets/${encodeURIComponent(sheet)}/today`, asOf)),
    enabled: sheet !== '',
  });
  return data?.today;
}

/** Whether the sheet is archived (everything visible, every change refused). Undefined while loading. */
export function useSheetArchived(sheet: string): boolean | undefined {
  const asOf = useAsOf();
  const { data } = useQuery({
    queryKey: sheetTodayKey(sheet, asOf),
    queryFn: () => getJson<SheetToday>(readPath(`/api/sheets/${encodeURIComponent(sheet)}/today`, asOf)),
    enabled: sheet !== '',
  });
  return data?.archived;
}
