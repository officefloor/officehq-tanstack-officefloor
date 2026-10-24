import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';

/** A sheet's holiday as the store holds it: a date (YYYY-MM-DD), on the sheet at most once. Adding one works out
 *  again the due-by of every claim not SETTLED or VOID whose due-by is on or after it, over the holidays now. */
export type Holiday = { id: number; date: string };

export const holidaysQueryKey = (sheetId: string) => ['sheet', sheetId, 'holidays'] as const;

export const holidaysPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/holidays`;

/** The sheet's holidays, by date. */
export function useHolidays() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: holidaysQueryKey(sheet ?? ''),
    queryFn: () => getJson<Holiday[]>(holidaysPath(sheet ?? '')),
    enabled: !!sheet,
  });
}
