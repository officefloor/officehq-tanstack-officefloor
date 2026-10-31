import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asOfKey, asOfPath, useAsOf } from '../asof/useAsOf';

/** A holiday as the server sends it: its date, yyyy-mm-dd. */
export type Holiday = { date: string };

export const holidaysKey = (sheet: string) => ['sheet', sheet, 'holidays'] as const;

/** The sheet's holidays, earliest first, re-read whenever the sheet is loaded. */
export function useHolidays(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(holidaysKey(sheet), asOf),
    queryFn: () => getJson<Holiday[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/holidays`, asOf)),
    staleTime: 0,
    gcTime: 0,
  });
}
