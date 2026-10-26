import { getJson } from '../../api/http';

/** `today` is the sheet's own date (yyyy-mm-dd): use it, never the real clock, for anything dated.
 *  An `archived` sheet shows everything and refuses every change. */
export type Sheet = { id: string; today: string; archived: boolean };

/** Every query kept per sheet lives under this key, so re-loading a sheet re-reads all of it. */
export const sheetKey = (sheetId: string) => ['sheet', sheetId] as const;

export const sheetQuery = (sheetId: string) => ({
  queryKey: sheetKey(sheetId),
  queryFn: () => getJson<Sheet>(`/api/sheets/${encodeURIComponent(sheetId)}`),
});
