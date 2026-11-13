import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey, useCurrentSheet } from '../session/session';

/** That `user` watches the person `personId`. */
export type Watch = { personId: number; user: string };

/** The query key for the sheet's watches; under sheetKey, so re-loading the sheet re-reads them. */
export const watchesKey = (sheet: string) => [...sheetKey(sheet), 'watches'] as const;

/** Every watch on the sheet, in the order they started. */
export function useWatches() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: watchesKey(sheet),
    queryFn: () => getJson<Watch[]>(`/api/sheets/${encodeURIComponent(sheet)}/watches`),
    enabled: !!sheet,
  });
}

/** Whether `user` watches the person. */
export const isWatching = (watches: Watch[], personId: number, user: string) =>
  watches.some((w) => w.personId === personId && w.user === user);
