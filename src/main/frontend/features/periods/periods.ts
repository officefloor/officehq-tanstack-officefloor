import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A period of the sheet: `name` is unique on the sheet (case ignored); `from` and `to` (yyyy-MM-dd) are both
 *  inclusive. No two periods of a sheet overlap. A closed period keeps `closedTotal`, the due-total fixed when it
 *  was closed; no claim dated inside it can be added, edited, opened, settled or voided. */
export type Period = {
  id: number;
  sheetId: string;
  name: string;
  from: string;
  to: string;
  closed: boolean;
  closedTotal: string | null;
};

export const periodsKey = (sheetId: string) => ['sheet', sheetId, 'periods'];

/** The sheet's periods, by from-date. */
export function usePeriods(sheetId: string) {
  return useQuery({
    queryKey: periodsKey(sheetId),
    queryFn: () => getJson<Period[]>(`/api/sheets/${encodeURIComponent(sheetId)}/periods`),
  });
}

/** The period whose dates contain a date (yyyy-MM-dd), or undefined when none does. */
export function periodOf(periods: Period[] | undefined, date: string): Period | undefined {
  return periods?.find((p) => p.from <= date && date <= p.to);
}
