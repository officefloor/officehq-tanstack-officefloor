import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import type { Grade } from '../people/people';

/** A grade's rate: a percent with two decimals, in force from its from-date (yyyy-MM-dd; null when it
 *  applies from the beginning of time) through its to-date (null when it has no end). */
export type Rate = { grade: Grade; pct: string; from: string | null; to: string | null };

export const ratesKey = (sheet: string) => ['rates', sheet] as const;

export const ratesPath = (sheet: string) => `/api/sheets/${encodeURIComponent(sheet)}/rates`;

/** Every rate on the sheet, by grade then from-date (the one with none first); a grade with no rate is absent. */
export function useRates(sheet: string) {
  return useQuery({
    queryKey: ratesKey(sheet),
    queryFn: () => getJson<Rate[]>(ratesPath(sheet)),
  });
}

/** The rate for a grade on a date (yyyy-MM-dd): the one whose from-date is the latest on or before it, or
 *  undefined when the date is before every rate the grade has, or after the to-date of that one (or the date
 *  is not yet known). */
export function rateOf(rates: Rate[], grade: Grade | undefined, date: string | undefined): string | undefined {
  if (date === undefined || grade === undefined) return undefined;
  let found: Rate | undefined;
  for (const r of rates) {
    if (r.grade !== grade || (r.from !== null && r.from > date)) continue;
    if (found === undefined || (found.from ?? '') < (r.from ?? '')) found = r;
  }
  if (found?.to != null && found.to < date) return undefined;
  return found?.pct;
}

/** A two-decimal string as a whole number of hundredths. */
export function hundredths(value: string): bigint {
  const negative = value.startsWith('-');
  const [whole, frac = ''] = value.replace(/^-/, '').split('.');
  const cents = BigInt(whole) * 100n + BigInt(frac.padEnd(2, '0').slice(0, 2));
  return negative ? -cents : cents;
}

/** A whole number of hundredths as a two-decimal string. */
export function twoDecimals(cents: bigint): string {
  const abs = cents < 0n ? -cents : cents;
  return `${cents < 0n ? '-' : ''}${abs / 100n}.${String(abs % 100n).padStart(2, '0')}`;
}

/** An amount times a percent, divided by 100, in hundredths rounded half to even (1.025 -> 1.02,
 *  1.035 -> 1.04), or half up when `halfUp` (1.025 -> 1.03); undefined when there is no rate. Worked in whole
 *  numbers so no float error shows. */
export function dueCents(amount: string, pct: string | undefined, halfUp = false): bigint | undefined {
  if (pct === undefined) return undefined;
  const product = hundredths(amount) * hundredths(pct);
  // Rounded on the magnitude, so a negative amount (a correction) rounds as its positive would.
  const abs = product < 0n ? -product : product;
  let quotient = abs / 10000n;
  const remainder = abs % 10000n;
  if (remainder > 5000n || (remainder === 5000n && (halfUp || quotient % 2n === 1n))) quotient += 1n;
  return product < 0n ? -quotient : quotient;
}

/** A due as the screen shows it: two decimals, or n/a when there is no rate. */
export function showDue(cents: bigint | undefined): string {
  return cents === undefined ? 'n/a' : twoDecimals(cents);
}
