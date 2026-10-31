import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asOfKey, asOfPath, useAsOf } from '../asof/useAsOf';

export type Sheet = { id: string; today: string; archived: boolean };

export const sheetKey = (sheet: string) => ['sheet', sheet] as const;

/** The active sheet as the store holds it, re-read whenever the sheet is loaded. */
export function useSheet(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(sheetKey(sheet), asOf),
    queryFn: () => getJson<Sheet>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}`, asOf)),
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
