import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { nowKey, useReadNow } from '../../query/readNow';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { useAsOf } from '../asof/asof.slot';
import { usePreview } from '../preview/preview';

// Who is signed in and which sheet is active live in the URL (`user`, `sheet`), so any feature
// reads them directly. There is no password: signing in is typing a name.
export const useUser = () => useSearchParam('user', asString);
export const useSheetId = () => useSearchParam('sheet', asString);

/** `today` (yyyy-MM-dd) is the sheet's own date: every date decision and default date on the
 *  sheet uses it, never the browser's clock. An `archived` sheet stays visible and refuses every change. `parent` is the
 *  id of its parent sheet (whose rates apply where it has none), null when none. */
export type Sheet = { id: string; today: string; archived: boolean; parent: string | null };

/** The active sheet as the store holds it. Keyed ['sheet', id]: every per-sheet key starts so. */
export function useSheet(sheetId: string) {
  const now = useReadNow();
  return useQuery({
    queryKey: nowKey(['sheet', sheetId], now),
    queryFn: () => getJson<Sheet>(`/api/sheets/${encodeURIComponent(sheetId)}`, now),
    enabled: sheetId !== '',
  });
}

/** The sheet's today (yyyy-MM-dd), or '' while the sheet loads, as every figure on this screen takes it: the preview
 *  date while this screen previews one (see features/preview; not while it shows the sheet as of a date, nor in a read
 *  of the sheet as it is now). Use it for every figure; a change and its defaults use the sheet's real today. */
export function useSheetToday(sheetId: string): string {
  const today = useSheet(sheetId).data?.today ?? '';
  const [preview] = usePreview();
  const [asOf] = useAsOf();
  const now = useReadNow();
  return today !== '' && preview !== '' && asOf === '' && !now ? preview : today;
}
