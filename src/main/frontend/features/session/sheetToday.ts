import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetInfoKey, useCurrentSheet } from './session';
import type { SheetInfo } from './session';

/**
 * The active sheet's today (yyyy-MM-dd), or '' while it loads. Every date-dependent decision and
 * every default date on screen uses this — never `new Date()`.
 */
export function useSheetToday(): string {
  return useSheetInfo().data?.today ?? '';
}

/** The active sheet as the store holds it (its id, today and whether it is archived). */
export function useSheetInfo() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: sheetInfoKey(sheet),
    queryFn: () => getJson<SheetInfo>(`/api/sheets/${encodeURIComponent(sheet)}`),
    enabled: !!sheet,
  });
}
