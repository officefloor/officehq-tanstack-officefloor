import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A holiday on the sheet: a date (yyyy-MM-dd). A claim's due-by fixed at its creation is moved past it. */
export type Holiday = { date: string };

export const holidaysKey = (sheetId: string) => ['sheet', sheetId, 'holidays'] as const;

/** The holidays on the sheet, by date. */
export function useHolidays(sheetId: string) {
  return useQuery({
    queryKey: holidaysKey(sheetId),
    queryFn: async () =>
      (await getJson<{ holidays: Holiday[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/holidays`)).holidays,
  });
}

/** A holiday's date as the screen takes it: yyyy-MM-dd. */
export const DATE = /^\d{4}-\d{2}-\d{2}$/;
