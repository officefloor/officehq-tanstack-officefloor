import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheetQuery';

/** A holiday on the sheet (yyyy-mm-dd). */
export type Holiday = { date: string };

/** Under the sheet's key, so re-loading the sheet re-reads its holidays. */
export const holidaysKey = (sheetId: string) => [...sheetKey(sheetId), 'holidays'] as const;

export const holidaysQuery = (sheetId: string) => ({
  queryKey: holidaysKey(sheetId),
  queryFn: () => getJson<Holiday[]>(`/api/sheets/${encodeURIComponent(sheetId)}/holidays`),
});
