import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useView, viewKey, viewPath } from '../asof/useAsOf';

/** `lastBatch`: the number of the sheet's last bulk settle batch, kept once its claims leave the sheet; null when none. */
export type Sheet = { id: string; today: string; archived: boolean; lastBatch: number | null };

export const sheetKey = (sheet: string) => ['sheet', sheet] as const;

/** The active sheet as the store holds it, re-read whenever the sheet is loaded. Its today is the
 *  preview date while one is set, unless `realToday` asks for the sheet's own. */
export function useSheet(sheet: string, realToday = false) {
  const shown = useView();
  const view = realToday ? { ...shown, preview: '' } : shown;
  return useQuery({
    queryKey: viewKey(sheetKey(sheet), view),
    queryFn: () => getJson<Sheet>(viewPath(`/api/sheets/${encodeURIComponent(sheet)}`, view)),
    staleTime: 0,
    gcTime: 0,
  });
}

/**
 * The sheet's today (yyyy-mm-dd). Every date-dependent decision and every default date on the
 * sheet uses this, never the browser's clock. '' until the sheet has loaded.
 */
export function useSheetToday(sheet: string): string {
  return useSheet(sheet).data?.today ?? '';
}
