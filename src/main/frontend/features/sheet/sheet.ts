import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { currentPreview, useAsOf } from '../../session/session';

/** `today` (yyyy-MM-dd) is the sheet's own date: every date decision and default uses it, never the clock.
 *  An `archived` sheet stays visible but the server refuses every change to it. `asOf` (yyyy-MM-dd) is the
 *  date this screen shows the sheet as of, null when it shows it as it is now; `archived` is then as it
 *  stood at the end of that date. `preview` (yyyy-MM-dd) is this screen's preview date, null when none is set:
 *  every figure is then read as if it were the sheet's today. */
export type Sheet = { id: string; today: string; archived: boolean; asOf: string | null; preview: string | null };

/** Every per-sheet query key starts with ['sheet', id], so loading a sheet re-reads all of it. */
export const sheetKey = (id: string) => ['sheet', id] as const;

/** A per-sheet query key as read for the screen's as-of date and preview date: the key itself when there is
 *  neither, so a change made (never while an as-of date is set) updates the very queries it read. While a preview
 *  date is set every change re-reads the sheet instead (query/queryClient). */
export const asOfKey = <K extends readonly unknown[]>(key: K, asOf: string) => {
  const preview = currentPreview();
  if (asOf === '' && preview === '') return key;
  return [...key, preview === '' ? { asOf } : { asOf, preview }] as unknown as K;
};

/** A read path with the screen's as-of date added (as `asOf`), so the server answers as it stood then. */
export const asOfPath = (path: string, asOf: string) =>
  asOf === '' ? path : `${path}${path.includes('?') ? '&' : '?'}asOf=${encodeURIComponent(asOf)}`;

export function useSheet(id: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(sheetKey(id), asOf),
    queryFn: () => getJson<Sheet>(asOfPath(`/api/sheets/${encodeURIComponent(id)}`, asOf)),
    enabled: id !== '',
  });
}

/** The sheet's today (yyyy-MM-dd), or undefined while the sheet loads. Use it for every default date.
 *  While the screen shows the sheet as of a date, or previews a date, that date stands in for it in every figure. */
export function useSheetToday(id: string): string | undefined {
  const data = useSheet(id).data;
  return data?.asOf ?? data?.preview ?? data?.today;
}

/** Under the sheet's key, so loading the sheet again re-reads its descendants. */
const descendantsKey = (sheet: string) => [...sheetKey(sheet), 'descendants'] as const;

/** The ids of the sheets naming the sheet as their parent, their children, and so on (as of the screen's
 *  as-of date, when one is set). */
export function useDescendants(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(descendantsKey(sheet), asOf),
    queryFn: () => getJson<string[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/descendants`, asOf)),
    enabled: sheet !== '',
  });
}
