import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useView, viewKey, viewPath } from '../asof/useAsOf';

export const watchesKey = (sheet: string, user: string) => ['sheet', sheet, 'watches', user] as const;

/** The ids of the people on the sheet the signed-in user watches, re-read whenever the sheet is loaded. */
export function useWatches(sheet: string, user: string) {
  const view = useView();
  return useQuery({
    queryKey: viewKey(watchesKey(sheet, user), view),
    queryFn: () =>
      getJson<number[]>(viewPath(`/api/sheets/${encodeURIComponent(sheet)}/watches?user=${encodeURIComponent(user)}`, view)),
    staleTime: 0,
    gcTime: 0,
  });
}
