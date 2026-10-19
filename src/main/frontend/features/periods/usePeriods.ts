import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

// The sheet's periods: a name unique on the sheet and a from and to date (yyyy-MM-dd), both
// inclusive, no two overlapping. A claim belongs to the period whose dates contain its date, or none.
// Kept apart from Periods.tsx so the claims can use it without importing the screen that totals them.
export type Period = { name: string; from: string; to: string };

export const periodsKey = (sheet: string) => ['sheet', sheet, 'periods'];
export const periodsPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/periods`;

export function usePeriods(sheet: string): Period[] {
  return (
    useQuery({
      queryKey: periodsKey(sheet),
      queryFn: () => getJson<Period[]>(periodsPath(sheet)),
      enabled: !!sheet,
    }).data ?? []
  );
}

/** Whether the date (yyyy-MM-dd) falls in the period, its dates included. */
export const inPeriod = (p: Period, date: string) => p.from <= date && date <= p.to;

/** The period the date falls in, or undefined when none does. */
export const periodOf = (periods: Period[], date: string): Period | undefined =>
  periods.find((p) => inPeriod(p, date));
