import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetInfoKey, useCurrentSheet } from './session';
import type { SheetInfo } from './session';

/**
 * The active sheet's today (yyyy-MM-dd), or '' while it loads. Every date-dependent decision and
 * every default date on screen uses this — never `new Date()`.
 */
export function useSheetToday(): string {
  const [sheet] = useCurrentSheet();
  const info = useQuery({
    queryKey: sheetInfoKey(sheet),
    queryFn: () => getJson<SheetInfo>(`/api/sheets/${encodeURIComponent(sheet)}`),
    enabled: !!sheet,
  });
  return info.data?.today ?? '';
}
