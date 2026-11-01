import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import type { Grade } from '../people/people';

/** A grade's rate: a percent with two decimals. */
export type Rate = { grade: Grade; pct: string };

export const ratesKey = (sheet: string) => ['rates', sheet] as const;

export const ratesPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/rates`;

/** Every grade's rate on the sheet; a grade with no rate is absent. */
export function useRates(sheet: string) {
  return useQuery({
    queryKey: ratesKey(sheet),
    queryFn: () => getJson<Rate[]>(ratesPath(sheet)),
  });
}

/** The rate for a grade, or undefined when the grade has none. */
export function rateOf(rates: Rate[], grade: Grade): string | undefined {
  return rates.find((r) => r.grade === grade)?.pct;
}

/** A two-decimal string as a whole number of hundredths. */
export function hundredths(value: string): bigint {
  const [whole, frac = ''] = value.split('.');
  return BigInt(whole) * 100n + BigInt(frac.padEnd(2, '0').slice(0, 2));
}

/** A whole number of hundredths as a two-decimal string. */
export function twoDecimals(cents: bigint): string {
  return `${cents / 100n}.${String(cents % 100n).padStart(2, '0')}`;
}

/** An amount times a percent, divided by 100, in hundredths rounded half up (amounts and rates are
 *  never negative, so half up is half away from zero); undefined when there is no rate. Worked in
 *  whole numbers so no float error shows. */
export function dueCents(amount: string, pct: string | undefined): bigint | undefined {
  if (pct === undefined) return undefined;
  return (hundredths(amount) * hundredths(pct) + 5000n) / 10000n;
}

/** A due as the screen shows it: two decimals, or n/a when there is no rate. */
export function showDue(cents: bigint | undefined): string {
  return cents === undefined ? 'n/a' : twoDecimals(cents);
}
