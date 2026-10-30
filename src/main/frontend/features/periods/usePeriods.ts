import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A period: a name unique on the sheet, from and to dates (YYYY-MM-DD, both inclusive). */
export type Period = { name: string; from: string; to: string };

export const periodsKey = (sheet: string) => ['sheet', sheet, 'periods'] as const;

/** The sheet's periods, by from-date. */
export function usePeriods(sheet: string): Period[] {
  const { data } = useQuery({
    queryKey: periodsKey(sheet),
    queryFn: () => getJson<Period[]>(`/api/sheets/${encodeURIComponent(sheet)}/periods`),
    enabled: sheet !== '',
  });
  return data ?? [];
}

/** The period whose dates contain a date; periods never overlap, so there is at most one. */
export function periodOf(periods: Period[], date: string): Period | undefined {
  return periods.find((p) => p.from <= date && date <= p.to);
}
