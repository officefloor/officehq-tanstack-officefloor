import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asOfKey, asOfPath, sheetKey } from '../sheet/sheet';
import { useAsOf } from '../../session/session';

/** A holiday on the sheet (yyyy-mm-dd). A claim's due-by, fixed when it is added, counts business days
 *  past the weekends and those on the sheet then. */
export type Holiday = { date: string };

/** Under the sheet's key, so loading the sheet again re-reads its holidays. */
export const holidaysKey = (sheet: string) => [...sheetKey(sheet), 'holidays'] as const;

export function useHolidays(sheet: string) {
  const [asOf] = useAsOf();
  return useQuery({
    queryKey: asOfKey(holidaysKey(sheet), asOf),
    queryFn: () => getJson<Holiday[]>(asOfPath(`/api/sheets/${encodeURIComponent(sheet)}/holidays`, asOf)),
    enabled: sheet !== '',
  });
}
