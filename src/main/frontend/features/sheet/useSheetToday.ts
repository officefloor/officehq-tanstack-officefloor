import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { readPath, useAsOf } from '../asof/useAsOf';
import { usePreview } from '../preview/usePreview';

export type SheetToday = { sheet: string; today: string; archived: boolean };

export const sheetTodayKey = (sheet: string, asOf = '') => ['sheet', sheet, 'today', asOf] as const;

/** The sheet's today as figures are shown on it (YYYY-MM-DD; the as-of date while one is set, else the preview date while
 *  one is set). Every date decision and default date on screen reads this — never `new Date()`. Undefined while loading. */
export function useSheetToday(sheet: string): string | undefined {
  const preview = usePreview();
  const today = useRealSheetToday(sheet);
  const asOf = useAsOf();
  return asOf === '' && preview !== '' && today !== undefined ? preview : today;
}

/** The sheet's today whatever the preview date (the as-of date while one is set): what changes, and the rules
 *  the server judges them by, go on. Undefined while loading. */
export function useRealSheetToday(sheet: string): string | undefined {
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
