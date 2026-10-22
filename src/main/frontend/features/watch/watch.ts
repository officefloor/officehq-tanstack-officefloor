import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

export const watchesKey = (sheetId: string, user: string) => ['sheet', sheetId, 'watches', user] as const;

/** The ids of the people on the sheet the user watches. */
export function useWatches(sheetId: string, user: string) {
  return useQuery({
    queryKey: watchesKey(sheetId, user),
    queryFn: () =>
      getJson<number[]>(`/api/sheets/${encodeURIComponent(sheetId)}/watches?user=${encodeURIComponent(user)}`),
  });
}
