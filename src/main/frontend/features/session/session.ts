import { useQuery } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';

// The session lives in the URL: `user` is the signed-in name (no password), `sheet` the active
// sheet id. Every feature that works per sheet or records who did something reads these.
export const useCurrentUser = () => useSearchParam('user', asString);
export const useCurrentSheet = () => useSearchParam('sheet', asString);

// `today` is the sheet's own date (yyyy-mm-dd). Every date decision and every default date in the
// app uses it — never the browser's or server's clock.
export type Sheet = { id: string; today: string };
export const sheetKey = (id: string) => ['sheet', id] as const;

export const loadSheet = (id: string) => postJson<Sheet>(`/api/sheets/${encodeURIComponent(id)}/load`, {});

/** The active sheet, as the store holds it (filled by the sheet picker when it loads a sheet). */
export function useSheet() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: sheetKey(sheet ?? ''),
    queryFn: () => loadSheet(sheet!),
    enabled: !!sheet,
    staleTime: Infinity,
  });
}

/** The active sheet's today (yyyy-mm-dd): use it for every date default and date decision. */
export const useSheetToday = () => useSheet().data?.today;
