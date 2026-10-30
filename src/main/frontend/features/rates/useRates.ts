import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';

/** A grade's rate: a percent shown with two decimals ("12.50"), in force from a date (YYYY-MM-DD;
 *  "" for from the beginning of time). */
export type Rate = { grade: string; rate: string; from: string };

export const ratesKey = (sheet: string) => ['sheet', sheet, 'rates'] as const;

/** The sheet's rates, by grade then from-date (from the beginning of time first). */
export function useRates(sheet: string): Rate[] {
  const { data } = useQuery({
    queryKey: ratesKey(sheet),
    queryFn: () => getJson<Rate[]>(`/api/sheets/${encodeURIComponent(sheet)}/rates`),
    enabled: sheet !== '',
  });
  return data ?? [];
}

/** The grade's rate in force on a date: the one with the latest from-date on or before it;
 *  undefined when none applies (or the date is not known yet). */
export function rateOn(rates: Rate[], grade: string, date: string | undefined): string | undefined {
  if (date === undefined) return undefined;
  let found: Rate | undefined;
  for (const r of rates) {
    if (r.grade === grade && r.from <= date && (found === undefined || r.from >= found.from)) found = r;
  }
  return found?.rate;
}
