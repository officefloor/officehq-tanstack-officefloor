import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheetQuery';

/** One watch: the signed-in user `user` watches the person `personId`. */
export type Watch = { personId: number; user: string };

/** Under the sheet's key, so re-loading the sheet re-reads its watches. */
export const watchesKey = (sheetId: string) => [...sheetKey(sheetId), 'watches'] as const;

export const watchesQuery = (sheetId: string) => ({
  queryKey: watchesKey(sheetId),
  queryFn: () => getJson<Watch[]>(`/api/sheets/${encodeURIComponent(sheetId)}/watches`),
});
