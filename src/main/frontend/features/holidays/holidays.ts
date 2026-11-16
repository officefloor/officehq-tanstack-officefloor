import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A holiday of the sheet: its `date` (yyyy-MM-dd), at most once on the sheet. A claim whose due-by, as it is added,
 *  falls on a holiday is due the next day that is not one; a holiday added later leaves existing due-by dates as
 *  they are. */
export type Holiday = { date: string };

export const holidaysKey = (sheetId: string) => ['sheet', sheetId, 'holidays'];

/** The sheet's holidays, by date. */
export function useHolidays(sheetId: string) {
  return useQuery({
    queryKey: holidaysKey(sheetId),
    queryFn: () => getJson<Holiday[]>(`/api/sheets/${encodeURIComponent(sheetId)}/holidays`),
  });
}
