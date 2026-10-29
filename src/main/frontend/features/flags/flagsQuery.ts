import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheetQuery';

/** A person's flag: its reason, in force while the sheet's today is on or before `until` (yyyy-mm-dd). */
export type Flag = { personId: number; reason: string; until: string; shown: string };

/** Under the sheet's key, so re-loading the sheet re-reads its flags. */
export const flagsKey = (sheetId: string) => [...sheetKey(sheetId), 'flags'] as const;

export const flagsQuery = (sheetId: string) => ({
  queryKey: flagsKey(sheetId),
  queryFn: () => getJson<Flag[]>(`/api/sheets/${encodeURIComponent(sheetId)}/flags`),
});
