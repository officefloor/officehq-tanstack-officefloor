import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A period on the sheet: its name (unique on the sheet) and its from and to date (yyyy-MM-dd), inclusive;
 *  whether it is closed, and its due-total fixed when closed (two decimals; null while open). */
export type Period = { name: string; from: string; to: string; closed: boolean; closedTotal: string | null };

export const periodsKey = (sheet: string) => ['periods', sheet] as const;

export const periodsPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/periods`;

/** Where a period is closed. */
export const closePeriodPath = (sheet: string, name: string) =>
  `${periodsPath(sheet)}/${encodeURIComponent(name)}/close`;

/** Every period on the sheet, by from-date; no two overlap. */
export function usePeriods(sheet: string) {
  return useQuery({
    queryKey: periodsKey(sheet),
    queryFn: () => getJson<Period[]>(periodsPath(sheet)),
  });
}

/** The period whose dates contain the date (yyyy-MM-dd), or undefined when none does. */
export function periodOf(periods: Period[], date: string): Period | undefined {
  return periods.find((p) => p.from <= date && date <= p.to);
}
