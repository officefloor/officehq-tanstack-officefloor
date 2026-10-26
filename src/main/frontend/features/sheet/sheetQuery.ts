import { getJson } from '../../api/http';

export type Sheet = { id: string };

/** Every query kept per sheet lives under this key, so re-loading a sheet re-reads all of it. */
export const sheetKey = (sheetId: string) => ['sheet', sheetId] as const;

export const sheetQuery = (sheetId: string) => ({
  queryKey: sheetKey(sheetId),
  queryFn: () => getJson<Sheet>(`/api/sheets/${encodeURIComponent(sheetId)}`),
});
