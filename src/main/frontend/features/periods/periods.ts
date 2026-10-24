import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { useCurrentSheet } from '../session/session';

/** A sheet's period as the store holds it: its name (unique on the sheet) and its from and to date
 *  (YYYY-MM-DD), inclusive. No two periods overlap. */
export type Period = { id: number; name: string; from: string; to: string };

export const periodsQueryKey = (sheetId: string) => ['sheet', sheetId, 'periods'] as const;

export const periodsPath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/periods`;

/** The sheet's periods, by from-date. */
export function usePeriods() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: periodsQueryKey(sheet ?? ''),
    queryFn: () => getJson<Period[]>(periodsPath(sheet ?? '')),
    enabled: !!sheet,
  });
}

/** The period whose dates contain a date (YYYY-MM-DD), or undefined for none. */
export function periodOn(periods: Period[] | undefined, date: string): Period | undefined {
  return (periods ?? []).find((p) => p.from <= date && date <= p.to);
}
