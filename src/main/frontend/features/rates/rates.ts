import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey, useCurrentSheet } from '../session/session';

/**
 * A grade's rate as the store holds it: a percent with two decimals, in force from its from-date
 * (yyyy-MM-dd; null: from the beginning of time) until the grade's next rate takes over.
 */
export type Rate = { grade: string; rate: string; from: string | null };

/** The fields the rate form submits; a blank from-date is from the beginning of time. */
export type RateInput = { grade: string; rate: string; from: string };

/** What a due shows when its grade has no rate. */
export const NA = 'n/a';

/** 0.00 or more, with at most two decimals. */
export const PERCENT = /^(\d{1,5}(\.\d{1,2})?|\.\d{1,2})$/;

/** The query key for the sheet's rates; under sheetKey, so re-loading the sheet re-reads them. */
export const ratesKey = (sheet: string) => [...sheetKey(sheet), 'rates'] as const;

/** The sheet's rates, by grade and then from-date (from the start first). */
export function useRates() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: ratesKey(sheet),
    queryFn: () => getJson<Rate[]>(`/api/sheets/${encodeURIComponent(sheet)}/rates`),
    enabled: !!sheet,
  });
}

/**
 * The grade's rate (two decimals) in force on the date: the one with the latest from-date on or
 * before it, a rate with no from-date applying from the start; undefined when none applies.
 */
export function rateFor(rates: Rate[], grade: string, date: string): string | undefined {
  let found: Rate | undefined;
  for (const r of rates) {
    if (r.grade !== grade || (r.from !== null && r.from > date)) continue;
    if (!found || (found.from ?? '') < (r.from ?? '')) found = r;
  }
  return found?.rate;
}

/** A two-decimal value as whole hundredths, exactly. */
const hundredths = (value: string) => Math.round(Number(value) * 100);

/**
 * An amount times a percent rate, divided by 100, rounded half up to two decimals; n/a with no
 * rate. Worked in whole hundredths so it is exact: cents × basis points / 10000.
 */
export function due(amount: string, rate: string | undefined): string {
  if (rate === undefined) return NA;
  const product = hundredths(amount) * hundredths(rate);
  const cents = Math.sign(product) * Math.floor((Math.abs(product) + 5000) / 10000);
  return (cents / 100).toFixed(2);
}

/** Dues added up, skipping n/a; with two decimals, added in cents so it is exact. */
export function total(dues: string[]): string {
  const cents = dues.filter((d) => d !== NA).reduce((sum, d) => sum + hundredths(d), 0);
  return (cents / 100).toFixed(2);
}
