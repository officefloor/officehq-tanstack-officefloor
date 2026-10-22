import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A period on the sheet: a name unique on the sheet and an inclusive from and to date (yyyy-mm-dd). No two
 *  periods overlap. */
export type Period = { name: string; from: string; to: string };

export const periodsKey = (sheetId: string) => ['sheet', sheetId, 'periods'] as const;

/** The sheet's periods, earliest first. */
export function usePeriods(sheetId: string) {
  return useQuery({
    queryKey: periodsKey(sheetId),
    queryFn: () => getJson<Period[]>(`/api/sheets/${encodeURIComponent(sheetId)}/periods`),
  });
}

/** The period whose dates contain the date (yyyy-mm-dd), or undefined when none does. */
export function periodOf(periods: Period[], date: string): Period | undefined {
  return periods.find((p) => p.from <= date && date <= p.to);
}
