import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheet';

/** Under the sheet's key, so loading the sheet again re-reads who the user watches. */
export const watchesKey = (sheet: string, user: string) => [...sheetKey(sheet), 'watches', user] as const;

/** The ids of the people the signed-in user watches on the sheet. */
export function useWatches(sheet: string, user: string) {
  return useQuery({
    queryKey: watchesKey(sheet, user),
    queryFn: () =>
      getJson<number[]>(`/api/sheets/${encodeURIComponent(sheet)}/watches?user=${encodeURIComponent(user)}`),
    enabled: sheet !== '',
  });
}
