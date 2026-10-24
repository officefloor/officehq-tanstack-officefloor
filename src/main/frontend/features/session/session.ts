import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';

// The signed-in name and the active sheet id live in the URL (search params `user` and `sheet`).
// Everything else in the app is kept per sheet: a feature reads the sheet id from here and keys
// its queries under ['sheet', sheetId, ...], so loading a sheet re-reads all of them.
export const useCurrentUser = () => useSearchParam('user', asString);
export const useCurrentSheet = () => useSearchParam('sheet', asString);

// The active sheet as the store holds it: its id, its today and whether it is archived (then every
// change on it is refused). Every date decision and default
// date in the app uses the sheet's today (useSheetToday), never the browser's clock. In an as-of view
// (asof.slot.tsx) it is the sheet as it stood at the end of `asOf`, and that date is its today. Its
// lastBatch is the number of its last bulk settle batch (null while it has had none).
export type SheetInfo = { id: string; today: string; archived: boolean; asOf: string | null; lastBatch: number | null };

export const sheetQueryKey = (sheetId: string) => ['sheet', sheetId, 'info'] as const;

export function useSheetInfo() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: sheetQueryKey(sheet ?? ''),
    queryFn: () => getJson<SheetInfo>(`/api/sheets/${encodeURIComponent(sheet ?? '')}`),
    enabled: !!sheet,
  });
}

/** The sheet's today (YYYY-MM-DD), or undefined until the sheet is loaded. */
export const useSheetToday = () => useSheetInfo().data?.today;
