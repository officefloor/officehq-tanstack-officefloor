import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

export const holidaysKey = (sheet: string) => ['sheet', sheet, 'holidays'] as const;

/** The sheet's holidays (YYYY-MM-DD), earliest first. */
export function useHolidays(sheet: string): string[] {
  const { data } = useQuery({
    queryKey: holidaysKey(sheet),
    queryFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/holidays`),
    enabled: sheet !== '',
  });
  return data ?? [];
}
