import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asOfKey, asOfPath, useAsOf } from '../asof/useAsOf';

export const watchesKey = (sheet: string, user: string) => ['sheet', sheet, 'watches', user] as const;

/** The ids of the people on the sheet the signed-in user watches, re-read whenever the sheet is loaded. */
export function useWatches(sheet: string, user: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(watchesKey(sheet, user), asOf),
    queryFn: () =>
      getJson<number[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/watches?user=${encodeURIComponent(user)}`, asOf)),
    staleTime: 0,
    gcTime: 0,
  });
}
