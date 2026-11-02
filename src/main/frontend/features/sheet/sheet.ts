import { useQuery } from '@tanstack/react-query';
import { useReadScope } from '../../query/live';
import { asString, useSearchParam } from '../../url/useSearchParam';

/** `lastBatch`: the number of the sheet's last bulk settle batch, null when there has been none. `tagsGone`: the tags
 *  claims carried on the sheet before leaving it (a transfer takes them away), sorted. */
export type Sheet = { id: string; today: string; archived: boolean; lastBatch: number | null; tagsGone: string[] };

export const sheetKey = (sheet: string) => ['sheet', sheet] as const;

/** The sheet's settings, its today among them. Every default date and date decision on screen
 *  reads `today` from here, never from the browser's clock. */
export function useSheet(sheet: string) {
  const read = useReadScope(sheetKey(sheet));
  return useQuery({
    queryKey: read.key,
    queryFn: () => read.get<Sheet>(`/api/sheets/${encodeURIComponent(sheet)}`),
  });
}

/** The sheet's today (yyyy-MM-dd) as the screen's figures take it, or undefined while it loads: the screen's preview
 *  date while one is set (and no as-of date, which takes precedence), else the sheet's today. The sheet itself always
 *  answers its real today, which every change uses. */
export function useSheetToday(sheet: string): string | undefined {
  const today = useSheet(sheet).data?.today;
  const [asof] = useSearchParam('asof', asString);
  const [preview] = useSearchParam('preview', asString);
  return today !== undefined && preview && !asof ? preview : today;
}
