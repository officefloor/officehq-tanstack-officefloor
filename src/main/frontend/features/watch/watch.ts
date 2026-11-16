import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** That `user` watches the person `personId`. */
export type Watch = { personId: number; user: string };

export const watchesKey = (sheetId: string) => ['sheet', sheetId, 'watches'];

/** Every watch on the sheet, in the order they were started. */
export function useWatches(sheetId: string) {
  return useQuery({
    queryKey: watchesKey(sheetId),
    queryFn: () => getJson<Watch[]>(`/api/sheets/${encodeURIComponent(sheetId)}/watches`),
  });
}
