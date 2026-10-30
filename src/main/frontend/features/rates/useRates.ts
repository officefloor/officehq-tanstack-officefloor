import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { readPath, useAsOf } from '../asof/useAsOf';

/** A grade's rate: a percent shown with two decimals ("12.50"), in force from a date (YYYY-MM-DD;
 *  "" for from the beginning of time) through a date ("" for no end). */
export type Rate = { grade: string; rate: string; from: string; to: string };

export const ratesKey = (sheet: string, asOf = '') => ['sheet', sheet, 'rates', asOf] as const;

/** The sheet's rates, by grade then from-date (from the beginning of time first). */
export function useRates(sheet: string): Rate[] {
  const asOf = useAsOf();
  const { data } = useQuery({
    queryKey: ratesKey(sheet, asOf),
    queryFn: () => getJson<Rate[]>(readPath(`/api/sheets/${encodeURIComponent(sheet)}/rates`, asOf)),
    enabled: sheet !== '',
  });
  return data ?? [];
}

/** The grade's rate in force on a date: the one with the latest from-date on or before it;
 *  undefined when none applies, when that one's to-date is before the date (or the date is not known yet). */
export function rateOn(rates: Rate[], grade: string, date: string | undefined): string | undefined {
  if (date === undefined) return undefined;
  let found: Rate | undefined;
  for (const r of rates) {
    if (r.grade === grade && r.from <= date && (found === undefined || r.from >= found.from)) found = r;
  }
  if (found === undefined || (found.to !== '' && found.to < date)) return undefined;
  return found.rate;
}
