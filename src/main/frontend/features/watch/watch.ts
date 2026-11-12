import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asOfKey, asOfPath, sheetKey } from '../sheet/sheet';
import { useAsOf } from '../../session/session';

/** Under the sheet's key, so loading the sheet again re-reads who the user watches. */
export const watchesKey = (sheet: string, user: string) => [...sheetKey(sheet), 'watches', user] as const;

/** The ids of the people the signed-in user watches on the sheet. */
export function useWatches(sheet: string, user: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(watchesKey(sheet, user), asOf),
    queryFn: () =>
      getJson<number[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/watches?user=${encodeURIComponent(user)}`, asOf)),
    enabled: sheet !== '',
  });
}
