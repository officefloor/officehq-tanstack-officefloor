import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useView, viewKey, viewPath } from '../asof/useAsOf';

/** A holiday as the server sends it: its date, yyyy-mm-dd. */
export type Holiday = { date: string };

export const holidaysKey = (sheet: string) => ['sheet', sheet, 'holidays'] as const;

/** The sheet's holidays, earliest first, re-read whenever the sheet is loaded. */
export function useHolidays(sheet: string) {
  const view = useView();
  return useQuery({
    queryKey: viewKey(holidaysKey(sheet), view),
    queryFn: () => getJson<Holiday[]>(viewPath(`/api/sheets/${encodeURIComponent(sheet)}/holidays`, view)),
    staleTime: 0,
    gcTime: 0,
  });
}
