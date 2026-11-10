import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

// Watching is per user: each signed-in user has their own watched people on a sheet.
export const watchesKey = (sheetId: string, user: string) => ['sheet', sheetId, 'watches', user] as const;

/** The ids of the people the signed-in user watches on the sheet. */
export function useWatches(sheetId: string, user: string) {
  return useQuery({
    queryKey: watchesKey(sheetId, user),
    queryFn: async () =>
      (await getJson<{ personIds: string[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/watches`, user)).personIds,
  });
}
