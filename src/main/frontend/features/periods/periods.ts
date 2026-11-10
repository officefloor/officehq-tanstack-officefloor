import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A period: its name (unique on the sheet) and its from and to dates (yyyy-MM-dd), inclusive. */
export type Period = { name: string; from: string; to: string };

export const periodsKey = (sheetId: string) => ['sheet', sheetId, 'periods'] as const;

/** The sheet's periods, by from-date. */
export function usePeriods(sheetId: string) {
  return useQuery({
    queryKey: periodsKey(sheetId),
    queryFn: async () =>
      (await getJson<{ periods: Period[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/periods`)).periods,
  });
}

/** The period whose dates contain the date (yyyy-MM-dd), or null when none does. */
export function periodOf(periods: Period[], date: string): Period | null {
  return periods.find((p) => p.from <= date && date <= p.to) ?? null;
}
