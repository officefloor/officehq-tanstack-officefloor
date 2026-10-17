import { useQuery } from '@tanstack/react-query';
import { getJson, postJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';

// A period is a name, unique on the sheet, and a from and to date (yyyy-mm-dd), inclusive; no two
// periods of a sheet overlap. A claim belongs to the period whose dates contain its date, or to none.
export type Period = { name: string; from: string; to: string };
export type PeriodInput = Period;

const base = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/periods`;
export const periodsKey = (sheet: string) => ['sheet', sheet, 'periods'] as const;

/** The active sheet's periods, by from-date. */
export function usePeriods() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: periodsKey(sheet ?? ''),
    queryFn: () => getJson<{ periods: Period[] }>(base(sheet!)).then((r) => r.periods),
    enabled: !!sheet,
  });
}

export const addPeriod = (sheet: string, user: string, input: PeriodInput) => postJson<Period>(base(sheet), input, user);

/** The period whose dates contain the date, or undefined. */
export const periodOf = (periods: Period[], date: string): Period | undefined =>
  periods.find((p) => p.from <= date && date <= p.to);

/** The name of the period a date (yyyy-mm-dd) falls in on the active sheet, or undefined. */
export function usePeriodOf() {
  const periods = usePeriods().data ?? [];
  return (date: string) => periodOf(periods, date)?.name;
}
