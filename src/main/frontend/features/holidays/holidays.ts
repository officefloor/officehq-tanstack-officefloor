import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey, useCurrentSheet } from '../session/session';

/** The query key for the sheet's holidays; under sheetKey, so re-loading the sheet re-reads them. */
export const holidaysKey = (sheet: string) => [...sheetKey(sheet), 'holidays'] as const;

/**
 * The sheet's holidays (yyyy-MM-dd), earliest first. A claim's due-by, fixed when it is created, that falls on one
 * moves to the next day that is not one; a holiday added later leaves existing due-by dates as they are.
 */
export function useHolidays() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: holidaysKey(sheet),
    queryFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/holidays`),
    enabled: !!sheet,
  });
}
