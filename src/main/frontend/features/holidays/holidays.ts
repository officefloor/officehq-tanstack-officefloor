import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

export const holidaysKey = (sheet: string) => ['holidays', sheet] as const;

export const holidaysPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/holidays`;

/** The sheet's holidays (yyyy-MM-dd), earliest first. */
export function useHolidays(sheet: string) {
  return useQuery({
    queryKey: holidaysKey(sheet),
    queryFn: () => getJson<string[]>(holidaysPath(sheet)),
  });
}
