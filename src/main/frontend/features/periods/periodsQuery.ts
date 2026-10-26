import { getJson } from '../../api/http';
import { sheetKey } from '../sheet/sheetQuery';

/**
 * A sheet's period: a name unique on the sheet, `from` and `to` (yyyy-mm-dd) inclusive. Periods never overlap.
 * A closed period's due-total was fixed when it was closed (`closedTotal`; null while open).
 */
export type Period = { name: string; from: string; to: string; closed: boolean; closedTotal: string | null };

/** Under the sheet's key, so re-loading the sheet re-reads its periods. */
export const periodsKey = (sheetId: string) => [...sheetKey(sheetId), 'periods'] as const;

/** The sheet's periods, by from-date. */
export const periodsQuery = (sheetId: string) => ({
  queryKey: periodsKey(sheetId),
  queryFn: () => getJson<Period[]>(`/api/sheets/${encodeURIComponent(sheetId)}/periods`),
});

/** The period whose dates contain the date (yyyy-mm-dd), or undefined when none does. */
export function periodOf(periods: Period[], date: string): Period | undefined {
  return periods.find((p) => p.from <= date && date <= p.to);
}
